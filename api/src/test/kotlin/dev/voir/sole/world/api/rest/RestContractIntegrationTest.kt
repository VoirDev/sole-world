package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.integration.ApiAccessKey
import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestContractIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `every endpoint requires an api key`() {
        val response = rest("/v1/countries", apiKey = null)

        assertProblem(response, status = 401)
        assertTrue(response.headers().firstValue("WWW-Authenticate").isPresent)
    }

    @Test
    fun `an unknown api key is rejected`() {
        assertProblem(rest("/v1/countries", apiKey = "not-a-configured-key"), status = 401)
    }

    @Test
    fun `the api key is also accepted as a bearer token`() {
        val response = send(
            path = "/v1/countries",
            apiKey = null,
            headers = mapOf("Authorization" to "Bearer ${ApiAccessKey.RAW}"),
        )

        assertEquals(200, response.statusCode())
    }

    @Test
    fun `the contract and its reference page are readable without a key`() {
        val spec = rest("/openapi.yaml", apiKey = null)
        assertEquals(200, spec.statusCode())
        assertTrue(spec.body().startsWith("openapi:"))

        assertEquals(200, rest("/docs", apiKey = null).statusCode())
        assertEquals(200, rest("/healthz", apiKey = null).statusCode())
    }

    @Test
    fun `responses carry a validator and revalidate to 304`() {
        val first = rest("/v1/countries?size=2")
        val etag = first.headers().firstValue("ETag").orElseThrow()

        assertEquals(200, first.statusCode())
        assertTrue(
            first.headers().firstValue("Cache-Control").orElse("").contains("private"),
            "a response fetched with a key is not for a shared cache to hand out",
        )
        assertTrue(
            first.headers().allValues("Vary").any { it.contains("Accept-Language") },
            "responses vary by language",
        )

        val revalidated = rest("/v1/countries?size=2", ifNoneMatch = etag)
        assertEquals(304, revalidated.statusCode())
        assertTrue(revalidated.body().isEmpty(), "a 304 carries no body")
    }

    @Test
    fun `the validator changes with the language and the query`() {
        val english = rest("/v1/countries?size=2").headers().firstValue("ETag").orElseThrow()
        val russian =
            rest("/v1/countries?size=2", acceptLanguage = "ru").headers().firstValue("ETag").orElseThrow()
        val otherPage = rest("/v1/countries?size=1").headers().firstValue("ETag").orElseThrow()

        assertFalse(english == russian, "a different language must not reuse a validator")
        assertFalse(english == otherPage, "a different query must not reuse a validator")
    }

    @Test
    fun `a stale validator is ignored`() {
        val response = rest("/v1/countries?size=2", ifNoneMatch = "\"not-the-current-etag\"")

        assertEquals(200, response.statusCode())
    }

    @Test
    fun `language comes from the header and the lang parameter overrides it`() {
        assertEquals("Freedonia", get("/v1/countries/FD")["name"].stringValue())
        assertEquals("Фридония", get("/v1/countries/FD", acceptLanguage = "ru")["name"].stringValue())
        assertEquals("Фридония", get("/v1/countries/FD?lang=ru")["name"].stringValue())

        // The explicit parameter wins, so a localized URL is self-describing.
        assertEquals(
            "Freedonia",
            get("/v1/countries/FD?lang=en", acceptLanguage = "ru")["name"].stringValue(),
        )
        assertEquals(
            "Фридония",
            get("/v1/countries/FD?lang=ru", acceptLanguage = "de")["name"].stringValue(),
        )
    }

    @Test
    fun `an unsupported language falls back to base data`() {
        assertEquals("Freedonia", get("/v1/countries/FD?lang=sv")["name"].stringValue())
    }

    @Test
    fun `meta reports the dataset and the limits every endpoint applies`() {
        val meta = get("/v1/meta")

        assertEquals(1, meta["datasetVersion"].intValue())
        assertEquals(50, meta["maxPageSize"].intValue())
        assertEquals(50, meta["maxIncludedItems"].intValue())
        assertEquals(2, meta.at("/counts/countries").intValue())
        assertEquals(2, meta.at("/counts/cities").intValue())

        assertEquals(1, meta.at("/counts/locales").intValue())

        val locales = mutableListOf<String>()
        for (value in meta["supportedLocales"]) {
            locales += value.stringValue()
        }

        assertEquals(listOf("ru"), locales)
        assertFalse(locales.contains("en"), "English is the base data, not a translation")
    }

    @Test
    fun `content language names the locale a response is written in`() {
        fun contentLanguage(path: String, acceptLanguage: String? = null) =
            rest(path, acceptLanguage = acceptLanguage).headers().firstValue("Content-Language").orElse(null)

        assertEquals("en", contentLanguage("/v1/countries/FD"))
        assertEquals("ru", contentLanguage("/v1/countries/FD", acceptLanguage = "ru-RU"))
        assertEquals("ru", contentLanguage("/v1/countries/FD?lang=ru", acceptLanguage = "de"))

        // An unsupported language is served base data rather than refused; the header says so.
        assertEquals("en", contentLanguage("/v1/countries/FD", acceptLanguage = "sv"))
    }

    @Test
    fun `a revalidated response keeps its content language`() {
        val etag = rest("/v1/countries/FD?lang=ru").headers().firstValue("ETag").orElseThrow()
        val revalidated = rest("/v1/countries/FD?lang=ru", ifNoneMatch = etag)

        assertEquals(304, revalidated.statusCode())
        assertEquals("ru", revalidated.headers().firstValue("Content-Language").orElse(null))
    }

    @Test
    fun `a problem document does not claim the requested language`() {
        // Problem details are always written in English, whatever the caller asked for.
        val response = rest("/v1/countries/XX?lang=ru")

        assertProblem(response, status = 404)
        assertFalse(response.headers().firstValue("Content-Language").isPresent)
    }

    @Test
    fun `every documented response is one the service can actually give`() {
        // The contract used to describe only the happy path plus 400, 401 and 404 — no 304, though
        // ETag semantics were described in prose and implemented, and no 500 though the handler
        // returns one. A response documented but never given is as misleading as one given but
        // never documented.
        val spec = rest("/openapi.yaml", apiKey = null).body()

        assertTrue(spec.contains("NotModified:"), "304 should be a shared response")
        assertTrue(spec.contains("InternalServerError:"), "500 should be a shared response")

        val operations = PATH_PATTERN.findAll(spec).count()
        for (status in listOf("\"200\"", "\"304\"", "\"401\"", "\"429\"", "\"500\"")) {
            val declared = Regex(Regex.escape("$status:")).findAll(spec).count()
            assertTrue(
                declared >= operations,
                "$status is declared $declared times for $operations documented paths",
            )
        }

        // And nothing claims a 503: the dataset is parsed at startup, so the service either serves
        // or is not listening.
        assertFalse(spec.contains("\"503\""), "the service never answers 503")
    }

    @Test
    fun `every documented path is routable`() {
        // Guards against the contract describing an endpoint nobody implemented.
        val spec = rest("/openapi.yaml", apiKey = null).body()
        val paths = PATH_PATTERN.findAll(spec).map { it.groupValues[1] }.toList()

        assertTrue(paths.size > 30, "expected the whole surface to be documented, found ${paths.size}")

        val failures = paths.mapNotNull { path ->
            val response = rest(resolvePlaceholders(path))
            if (response.statusCode() == 200) null else "$path -> ${response.statusCode()}"
        }

        assertTrue(failures.isEmpty(), "documented paths that did not resolve: $failures")
    }

    /** Substitutes fixture identifiers for the path placeholders in the contract. */
    private fun resolvePlaceholders(path: String): String {
        val identifier = when {
            path.startsWith("/v1/countries/") -> "FD"
            path.startsWith("/v1/currencies/") -> "FDC"
            path.startsWith("/v1/cryptos/") -> "freecoin"
            path.startsWith("/v1/languages/") -> "fd"
            path.startsWith("/v1/locales/") -> "ru"
            path.startsWith("/v1/regions/") -> "test-europe"
            path.startsWith("/v1/subregions/") -> "test-north"
            path.startsWith("/v1/central-banks/") -> "freedonian-reserve"
            path.startsWith("/v1/states/") -> "FD-NF"
            path.startsWith("/v1/cities/") -> "601"
            path.startsWith("/v1/timezones/") -> "Europe/Freedonia"
            path.startsWith("/v1/flags/") -> "fd"
            path.startsWith("/v1/media-assets/") -> "flag-fd-square"
            else -> ""
        }

        return path.replace(PLACEHOLDER_PATTERN, identifier)
    }

    private companion object {
        /** Matches the two-space-indented path keys of the contract's `paths` section. */
        val PATH_PATTERN = Regex("""^ {2}(/v1/[^:\s]*):$""", RegexOption.MULTILINE)
        val PLACEHOLDER_PATTERN = Regex("""\{[a-zA-Z]+}""")
    }
}
