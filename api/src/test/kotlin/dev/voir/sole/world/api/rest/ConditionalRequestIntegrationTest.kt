package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

/**
 * Verifies that a conditional request costs nothing beyond its validator.
 *
 * The `ETag` was computed after the handler had already built the whole payload, so `If-None-Match`
 * saved serialization and bandwidth but none of the work: a `304` on a large collection took as long
 * as the `200` it replaced, while the class docstring and the README both claimed otherwise.
 */
class ConditionalRequestIntegrationTest : BaseRestIntegrationTest() {
    @MockitoSpyBean
    private lateinit var countryStore: CountryStore

    @Test
    fun `a matching validator is answered without reading the dataset`() {
        val path = "/v1/countries?size=2"
        val etag = rest(path).headers().firstValue("ETag").orElseThrow()

        clearInvocations(countryStore)
        val revalidated = rest(path, ifNoneMatch = etag)

        assertEquals(304, revalidated.statusCode())
        verifyNoInteractions(countryStore)
    }

    @Test
    fun `a 304 still carries the headers a cache needs`() {
        val path = "/v1/countries?size=2"
        val etag = rest(path).headers().firstValue("ETag").orElseThrow()

        val revalidated = rest(path, ifNoneMatch = etag)

        assertEquals(etag, revalidated.headers().firstValue("ETag").orElse(""))
        assertTrue(
            revalidated.headers().firstValue("Cache-Control").orElse("").contains("max-age"),
            "a 304 refreshes the freshness of the cached copy",
        )
        assertTrue(
            revalidated.headers().allValues("Vary").any { it.contains("Accept-Language") },
            "a 304 must vary the same way the 200 did",
        )
        assertTrue(revalidated.body().isEmpty(), "a 304 carries no body")
    }

    @Test
    fun `a validator from another representation does not match`() {
        val english = rest("/v1/countries?size=2").headers().firstValue("ETag").orElseThrow()

        val russian = rest("/v1/countries?size=2", acceptLanguage = "ru", ifNoneMatch = english)

        assertEquals(200, russian.statusCode())
        assertEquals(200, rest("/v1/countries?size=3", ifNoneMatch = english).statusCode())
        assertEquals(200, rest("/v1/currencies?size=2", ifNoneMatch = english).statusCode())
    }

    @Test
    fun `one of several offered validators is enough`() {
        val path = "/v1/countries?size=2"
        val etag = rest(path).headers().firstValue("ETag").orElseThrow()

        val revalidated = rest(path, ifNoneMatch = """"stale-one", $etag, "stale-two"""")

        assertEquals(304, revalidated.statusCode())
    }
}
