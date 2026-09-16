package dev.voir.sole.world.api.security

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.http.HttpResponse

/**
 * Verifies the response headers that hold across every route.
 *
 * Problem documents echo part of the request back — an unknown `include` value, the request URI —
 * which is safe only while a browser treats the response as what its content type says it is.
 * `nosniff` is what makes that true, so it has to be present on the failures too, not just on the
 * successful responses.
 */
class SecurityHeadersIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `every route carries them, whatever it answers`() {
        for (response in listOf(
            rest("/v1/countries/1"),
            rest("/v1/countries/999"),
            rest("/v1/countries?include=bogus"),
            rest("/v1/countries/1", apiKey = null),
            rest("/healthz", apiKey = null),
            rest("/docs", apiKey = null),
            rest("/assets/flags/fd_1x1.svg", apiKey = null),
        )) {
            assertHeader(response, "X-Content-Type-Options", "nosniff")
            assertHeader(response, "Referrer-Policy", "no-referrer")
        }
    }

    @Test
    fun `a reflected include value is served as a problem document, not as markup`() {
        val response = rest("/v1/countries?include=%3Cscript%3E")

        assertEquals(400, response.statusCode())
        assertEquals(
            "application/problem+json",
            response.headers().firstValue("Content-Type").orElse("").substringBefore(';'),
        )
        assertHeader(response, "X-Content-Type-Options", "nosniff")
    }

    private fun assertHeader(response: HttpResponse<String>, name: String, expected: String) {
        assertEquals(
            expected,
            response.headers().firstValue(name).orElse(""),
            "$name on ${response.uri()} (${response.statusCode()})",
        )
    }
}
