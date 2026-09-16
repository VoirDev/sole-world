package dev.voir.sole.world.api.flag

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

/**
 * Covers flags behaving like every other browsable collection.
 *
 * `FlagStore` ordered by id and accepted no `query`, so a flag could not be found by caption at all
 * — which broke the promise the rest of the API keeps, that learning one endpoint teaches the rest.
 */
class FlagIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `flags are ordered by caption, not by id`() {
        val captions = captions(get("/v1/flags"))

        assertEquals(captions.sorted(), captions, "flags should read in caption order")
    }

    @Test
    fun `a flag can be found by caption`() {
        assertEquals(listOf("Sylvania flag"), captions(get("/v1/flags?query=Sylvania")))
    }

    @Test
    fun `a flag can be found by the emoji a caller has in hand`() {
        // The one thing a caller is likely to have and unable to name.
        assertEquals(listOf("Freedonia flag"), captions(get("/v1/flags?query=FD")))
    }

    @Test
    fun `flag search tolerates a typo like every other resource`() {
        assertEquals(listOf("Sylvania flag"), captions(get("/v1/flags?query=Sylvana%20flag")))
    }

    @Test
    fun `a search that matches nothing is an empty page, not an error`() {
        assertEquals(0, get("/v1/flags?query=nothingmatchesthis").at("/page/totalItems").intValue())
    }

    @Test
    fun `graphql searches flags the same way`() {
        val response = send(
            path = "/graphql",
            body = objectMapper.writeValueAsString(
                mapOf("query" to """query { flags(query: "Sylvania") { items { caption } } }"""),
            ),
        )

        assertTrue(response.statusCode() == 200, response.body())
        assertEquals(
            listOf("Sylvania flag"),
            captionsOf(objectMapper.readTree(response.body()).at("/data/flags/items")),
        )
    }

    private fun captions(page: JsonNode): List<String> = captionsOf(page["items"])

    private fun captionsOf(items: JsonNode): List<String> {
        val captions = mutableListOf<String>()
        for (item in items) {
            captions += item["caption"].stringValue()
        }

        return captions
    }
}
