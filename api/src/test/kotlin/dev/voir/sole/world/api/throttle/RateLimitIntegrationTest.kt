package dev.voir.sole.world.api.throttle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies that the rate limit is wired to the routes it claims to guard.
 *
 * Runs in its own application context with a burst of two, so the limit is reachable without the
 * test having to make hundreds of requests. Each test presents its own key, which is what gives it
 * its own allowance and keeps the tests independent of one another's order.
 * [RateLimiterTest] covers the bucket arithmetic.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "api.security.keys=throttle-key-0123456789, health-key-0123456789, unauth-key-0123456789",
        "dataset.root=test-dataset",
        "api.rate-limit.enabled=true",
        "api.rate-limit.requests-per-minute=60",
        "api.rate-limit.burst=2",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `a caller past their allowance gets a problem document and a Retry-After`() {
        val key = "throttle-key-0123456789"
        repeat(2) { assertEquals(200, get("/v1/countries?size=1", key).statusCode()) }

        val throttled = get("/v1/countries?size=1", key)

        assertEquals(429, throttled.statusCode())
        assertEquals(
            "application/problem+json",
            throttled.headers().firstValue("Content-Type").orElse("").substringBefore(';'),
        )
        assertTrue(throttled.headers().firstValue("Retry-After").isPresent, "Retry-After is required")
        assertTrue(throttled.body().contains("\"status\":429"), throttled.body())
    }

    @Test
    fun `the health check answers however busy the service is`() {
        val key = "health-key-0123456789"
        repeat(10) { get("/v1/countries?size=1", key) }

        assertEquals(200, get("/healthz", key).statusCode())
    }

    @Test
    fun `an unauthenticated request is rejected without spending anyone's allowance`() {
        repeat(10) { assertEquals(401, get("/v1/countries?size=1", apiKey = null).statusCode()) }

        assertEquals(200, get("/v1/countries?size=1", "unauth-key-0123456789").statusCode())
    }

    private fun get(path: String, apiKey: String?): HttpResponse<String> {
        val builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path")).GET()
        apiKey?.let { builder.header("X-API-KEY", it) }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
