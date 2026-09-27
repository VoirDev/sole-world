package dev.voir.sole.world.api.mediaasset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies that an operator can front the bundled assets with a CDN.
 *
 * The base URL is read at startup, so this needs its own application context.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "api.security.keys=sole-test-integration-key",
        "dataset.root=test-dataset",
        "api.assets.base-url=https://cdn.example.com",
        "api.rate-limit.enabled=false",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssetBaseUrlIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private val objectMapper = ObjectMapper()

    @Test
    fun `asset paths point at the configured origin everywhere they appear`() {
        val asset = get("/v1/media-assets/flag-fd-square")
        assertEquals(
            "https://cdn.example.com/assets/flags/fd_1x1.svg",
            asset.at("/image/formats/svg").stringValue(),
        )
        assertEquals(
            "https://cdn.example.com/fd_64.png",
            asset.at("/image/formats/png/xs").stringValue(),
        )

        // The same asset reached through a relationship, rather than its own endpoint.
        val flag = get("/v1/flags/fd?include=squareAsset")
        assertEquals(
            "https://cdn.example.com/assets/flags/fd_1x1.svg",
            flag.at("/squareAsset/image/formats/svg").stringValue(),
        )
    }

    private fun get(path: String): tools.jackson.databind.JsonNode {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("X-API-KEY", "sole-test-integration-key")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode(), response.body())

        return objectMapper.readTree(response.body())
    }
}
