package dev.voir.sole.world.api.integration

import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Shared wiring for API integration tests.
 *
 * Both transports run against the same small fixture dataset rather than the bundled world data, so
 * assertions can name exact records. The application context is identical for every subclass, which
 * lets Spring cache and reuse it across the whole suite.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "api.security.keys=sole-test-integration-key",
        "dataset.root=test-dataset",
        // A fixture asset, so that the /assets route is exercised without the bundled 113 MB.
        "api.assets.directory=src/test/resources/test-assets",
        // The suite makes hundreds of requests as one caller; throttling is covered on its own.
        "api.rate-limit.enabled=false",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseApiIntegrationTest {
    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    /**
     * Sends a request to the running application.
     * @param path Path and query string, starting with a slash.
     * @param apiKey Key sent as `X-API-KEY`, or null to send none.
     * @param headers Extra request headers.
     * @param body Request body; the request is a GET when null.
     * @return Raw HTTP response.
     */
    protected fun send(
        path: String,
        apiKey: String? = ApiAccessKey.RAW,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): HttpResponse<String> {
        val builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path"))

        if (body == null) {
            builder.GET()
        } else {
            builder.header("Content-Type", "application/json")
            builder.POST(HttpRequest.BodyPublishers.ofString(body))
        }

        apiKey?.let { builder.header("X-API-KEY", it) }
        headers.forEach(builder::header)

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
