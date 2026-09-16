package dev.voir.sole.world.api.console

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies that the developer consoles are served entirely by this deployment.
 *
 * Both used to load their JavaScript from a CDN, which ran third-party code on the API's own origin
 * — on `/docs`, a route that is public by default — and left both consoles blank wherever there is
 * no outbound internet access. Everything they need is now vendored into the image.
 */
class ConsolesIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `the reference page loads nothing from another origin`() {
        assertSelfContained(page("/docs"))
    }

    @Test
    fun `the graphiql console loads nothing from another origin`() {
        assertSelfContained(page("/graphiql"))
    }

    @Test
    fun `console pages declare a policy that confines them to this origin`() {
        for (path in listOf("/docs", "/graphiql")) {
            val policy = rest(path, apiKey = null).headers()
                .firstValue("Content-Security-Policy")
                .orElse("")

            assertTrue(policy.contains("default-src 'self'"), "$path: $policy")
            assertTrue(policy.contains("connect-src 'self'"), "$path: $policy")
            assertTrue(policy.contains("frame-ancestors 'none'"), "$path: $policy")
        }
    }

    @Test
    fun `the vendored bundles are served`() {
        for (path in VENDORED_ASSETS) {
            val response = rest(path, apiKey = null)

            assertEquals(200, response.statusCode(), path)
            assertTrue(response.body().isNotEmpty(), "$path served nothing")
        }
    }

    @Test
    fun `nothing outside the vendored set is reachable`() {
        // The file name is matched against what the console ships rather than resolved as a path.
        for (path in listOf(
            "/docs/assets/nothing.js",
            "/docs/assets/application.yml",
            "/graphiql/assets/graphiql.min.js.unknown",
        )) {
            assertEquals(404, rest(path, apiKey = null).statusCode(), path)
        }

        // An encoded separator never reaches a handler at all; the container rejects it first.
        assertEquals(400, rest("/graphiql/assets/..%2f..%2fapplication.yml", apiKey = null).statusCode())
    }

    @Test
    fun `consoles are reachable without a key`() {
        // A caller cannot be asked to authenticate before being allowed to read what the API offers.
        assertEquals(200, rest("/docs", apiKey = null).statusCode())
        assertEquals(200, rest("/graphiql", apiKey = null).statusCode())
        assertEquals(200, rest("/openapi.yaml", apiKey = null).statusCode())
    }

    private fun page(path: String): String {
        val response = rest(path, apiKey = null)
        assertEquals(200, response.statusCode(), path)

        return response.body()
    }

    private fun assertSelfContained(page: String) {
        // Comments explain why the CDNs are gone, so they are not part of what the browser loads.
        val markup = page.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val external = Regex("""(?:src|href)="(?:https?:)?//[^"]+"""").findAll(markup)
            .map { it.value }
            .toList()

        assertTrue(external.isEmpty(), "console page still loads $external")
        assertFalse(markup.contains("esm.sh"), "console page still references esm.sh")
        assertFalse(markup.contains("cdn.jsdelivr.net"), "console page still references jsdelivr")
    }

    private companion object {
        val VENDORED_ASSETS = listOf(
            "/docs/assets/api-reference.js",
            "/graphiql/assets/graphiql.min.js",
            "/graphiql/assets/graphiql.min.css",
            "/graphiql/assets/react.production.min.js",
            "/graphiql/assets/react-dom.production.min.js",
        )
    }
}
