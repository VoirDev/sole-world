package dev.voir.sole.world.api.security

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the boundary between what is public and what needs a key.
 *
 * [PublicPaths] matches on the request path, so the question is whether a path can be written in a
 * way that matches the allowlist while being routed somewhere else. These cases were checked by hand
 * during an audit; they are here so they stay checked.
 */
class PublicPathsIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `the public surface needs no key`() {
        for (path in listOf("/healthz", "/openapi.yaml", "/docs", "/graphiql", "/assets/flags/fd_1x1.svg")) {
            assertEquals(200, rest(path, apiKey = null).statusCode(), path)
        }
    }

    @Test
    fun `everything else needs one`() {
        for (path in listOf("/v1/meta", "/v1/countries", "/v1/countries/FD", "/v1/cities?size=1")) {
            assertEquals(401, rest(path, apiKey = null).statusCode(), path)
        }
    }

    @Test
    fun `a path cannot be dressed up as a public one`() {
        // Each of these reads as "under /assets/" but is not; none may serve data.
        for (path in listOf(
            "/assets/..%2fv1%2fcountries",
            "/assets/%2e%2e/v1/countries",
            "/assets/../v1/countries",
            "/v1/countries;/assets/",
            "/assets//../v1/countries",
        )) {
            val response = rest(path, apiKey = null)

            assertTrue(
                response.statusCode() in listOf(400, 401, 404),
                "$path answered ${response.statusCode()}",
            )
            assertFalse(
                response.body().contains("\"items\""),
                "$path served data without a key",
            )
        }
    }

    @Test
    fun `the allowlist is case sensitive, because routing is`() {
        assertEquals(401, rest("/V1/COUNTRIES", apiKey = null).statusCode())
        assertEquals(401, rest("/ASSETS/flags/fd_1x1.svg", apiKey = null).statusCode())
    }

    @Test
    fun `graphql introspection is not a way around the key`() {
        val response = send(
            path = "/graphql",
            apiKey = null,
            body = """{"query":"{ __schema { types { name } } }"}""",
        )

        assertEquals(401, response.statusCode())
        assertFalse(response.body().contains("__schema"), "the schema leaked to an unauthenticated caller")
    }
}
