package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

/**
 * Covers the limits that stop one small document from occupying a worker thread.
 *
 * These run against the limits this image ships with rather than test-only ones, because the
 * question they answer is whether a default deployment is safe.
 *
 * Complexity used to count fields, so aliasing was free: twenty aliased single-item city listings
 * looked trivial and cost twenty scans of the largest entity in the dataset. Field weights now come
 * from the schema's `@cost` directive, so the limit counts scans.
 */
class QueryLimitsIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `aliasing an expensive listing many times is rejected`() {
        val aliases = (1..20).joinToString("\n") {
            "a$it: cities(page: { size: 1 }) { items { id name } }"
        }

        assertRejected(graphQL("query { $aliases }"), "complexity")
    }

    @Test
    fun `aliasing a cheap lookup many times is allowed`() {
        // The limit charges for scans, not for syntax, so repeating a by-id lookup stays cheap.
        val aliases = (1..20).joinToString("\n") { "a$it: country(idOrCode: \"1\") { id name }" }

        assertNoErrors(graphQL("query { $aliases }"))
    }

    @Test
    fun `an ordinary nested query is well inside the limit`() {
        assertNoErrors(
            graphQL(
                """
                query {
                  countries(page: { size: 50 }) {
                    items {
                      id
                      name
                      flag { id emoji }
                      currencies { id iso3 }
                      states(page: { size: 50 }) { items { id name } }
                      cities(page: { size: 50 }) { items { id name } }
                    }
                    pageInfo { totalItems hasNextPage }
                  }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `an over nested document is rejected`() {
        // Country and Subregion point at each other, so a document can be nested without end.
        val nested = (1..20).fold("id") { inner, _ -> "subregion { countries { $inner } }" }

        assertRejected(graphQL("query { country(idOrCode: 1) { $nested } }"), "maximum query depth")
    }

    private fun assertRejected(response: JsonNode, expectedInMessage: String) {
        val errors = response["errors"]
        assertTrue(errors != null && !errors.isEmpty, "expected a rejection: ${response.toPrettyString()}")

        val message = errors[0]["message"].stringValue()
        assertTrue(
            message.contains(expectedInMessage, ignoreCase = true),
            "expected a '$expectedInMessage' rejection, got: $message",
        )
        assertTrue(isNullOrMissing(response["data"]))
    }
}
