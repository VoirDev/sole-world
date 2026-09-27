package dev.voir.sole.world.api.security

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
 * Covers the documented deployment mode where the API is served with no authentication at all.
 *
 * It runs in its own application context because the flag is read at startup, and it exists because
 * two behaviours hinge on it: every route becomes reachable without a key, and responses become
 * publicly cacheable, which they must not be while a key is required.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "api.security.enabled=false",
        "dataset.root=test-dataset",
        "api.rate-limit.enabled=false",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicDeploymentIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `every route is reachable without a key`() {
        assertEquals(200, get("/v1/countries/FD").statusCode())
        assertEquals(200, get("/v1/meta").statusCode())
    }

    @Test
    fun `responses are publicly cacheable because nothing gates them`() {
        val cacheControl = get("/v1/countries/FD").headers().firstValue("Cache-Control").orElse("")

        assertTrue(cacheControl.contains("public"), cacheControl)
        assertTrue(cacheControl.contains("max-age"), cacheControl)
    }

    private fun get(path: String): HttpResponse<String> = httpClient.send(
        HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path")).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    )
}
