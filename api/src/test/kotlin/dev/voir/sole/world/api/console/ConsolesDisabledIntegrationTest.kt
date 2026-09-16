package dev.voir.sole.world.api.console

import dev.voir.sole.world.api.integration.ApiAccessKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies that switching the developer consoles off actually removes them.
 *
 * This runs in its own application context because the flags are read at startup: a deployment that
 * sets them cannot have the routes reappear later.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "api.security.keys=sole-test-integration-key",
        "dataset.root=test-dataset",
        "api.docs.enabled=false",
        "api.graphiql.enabled=false",
        "api.rate-limit.enabled=false",
        "logging.level.root=WARN",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsolesDisabledIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `the openapi contract is not served`() {
        assertNotServed("/openapi.yaml")
    }

    @Test
    fun `the reference page is not served`() {
        assertNotServed("/docs")
        // The page is held outside the static resource path, so nothing serves it by accident.
        assertNotServed("/docs/index.html")
        assertNotServed("/docs/docs.html")
    }

    @Test
    fun `the graphiql console is not served`() {
        assertNotServed("/graphiql")
        assertNotServed("/graphiql/index.html")
    }

    @Test
    fun `the consoles' vendored assets go with them`() {
        assertNotServed("/docs/assets/api-reference.js")
        assertNotServed("/graphiql/assets/graphiql.min.js")
    }

    @Test
    fun `the apis themselves still work`() {
        assertEquals(200, get("/v1/countries/1", withKey = true).statusCode())
        assertEquals(200, get("/healthz", withKey = false).statusCode())

        val graphql = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/graphql"))
            .header("Content-Type", "application/json")
            .header("X-API-KEY", ApiAccessKey.RAW)
            .POST(HttpRequest.BodyPublishers.ofString("""{"query":"{ country(idOrCode: 1) { name } }"}"""))
            .build()

        val response = httpClient.send(graphql, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
    }

    /**
     * Asserts a disabled console route is gone.
     *
     * Without a key the path is no longer public, so it answers `401`; with a key there is no
     * handler, so it answers `404`. Either way nothing is served.
     */
    private fun assertNotServed(path: String) {
        assertEquals(401, get(path, withKey = false).statusCode(), "$path should not be public")
        assertEquals(404, get(path, withKey = true).statusCode(), "$path should not exist")
    }

    private fun get(path: String, withKey: Boolean): HttpResponse<String> {
        val builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path")).GET()
        if (withKey) {
            builder.header("X-API-KEY", ApiAccessKey.RAW)
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
