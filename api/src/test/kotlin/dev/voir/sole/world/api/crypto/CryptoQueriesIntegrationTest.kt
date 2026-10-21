package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

/** Covers the cryptocurrency resource on the GraphQL transport. */
class CryptoQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `a coin carries every published field`() {
        val response = graphQL(
            """
            query {
              crypto(id: "freecoin") {
                id code name description websiteUrl introducedYear decimalDigits
                popularity obsolete obsoleteAt logoId
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val coin = response.at("/data/crypto")
        assertEquals("freecoin", coin["id"].stringValue())
        assertEquals("frc", coin["code"].stringValue())
        assertEquals("Freecoin", coin["name"].stringValue())
        assertEquals("https://freecoin.example", coin["websiteUrl"].stringValue())
        assertEquals(2011, coin["introducedYear"].intValue())
        assertEquals(8, coin["decimalDigits"].intValue())
        assertEquals(60, coin["popularity"].intValue())
        assertEquals("flag-fd-square", coin["logoId"].stringValue())
    }

    @Test
    fun `a coin resolves its logo`() {
        val response =
            graphQL("""query { crypto(id: "freecoin") { logo { id image { formats { svg } } } } }""")

        assertNoErrors(response)
        assertEquals("flag-fd-square", response.at("/data/crypto/logo/id").stringValue())
        assertEquals(
            "/assets/flags/fd_1x1.svg",
            response.at("/data/crypto/logo/image/formats/svg").stringValue(),
        )
    }

    @Test
    fun `a coin without a logo resolves to null`() {
        val response = graphQL("""query { crypto(id: "sylvacoin") { name logoId logo { id } } }""")

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/crypto/logo")))
        assertTrue(isNullOrMissing(response.at("/data/crypto/logoId")))
    }

    @Test
    fun `coins are listed by popularity and can be filtered by obsolescence`() {
        assertEquals(
            listOf("Freecoin", "Sylvacoin", "Oldcoin"),
            names(graphQL("query { cryptos { items { name } } }")),
        )
        assertEquals(
            listOf("Oldcoin"),
            names(graphQL("query { cryptos(obsolete: true) { items { name } } }")),
        )
    }

    @Test
    fun `sort chooses another ordering`() {
        val response = graphQL(
            """
            query {
              byName:     cryptos(sort: NAME)                   { items { name } }
              byNameDesc: cryptos(sort: NAME, order: DESC)      { items { name } }
              byYear:     cryptos(sort: YEAR)                   { items { name } }
              leastUsed:  cryptos(sort: POPULARITY, order: ASC) { items { name } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val data = response["data"]
        assertEquals(listOf("Freecoin", "Oldcoin", "Sylvacoin"), itemNames(data.at("/byName/items")))
        assertEquals(listOf("Sylvacoin", "Oldcoin", "Freecoin"), itemNames(data.at("/byNameDesc/items")))
        assertEquals(listOf("Oldcoin", "Freecoin", "Sylvacoin"), itemNames(data.at("/byYear/items")))
        assertEquals(listOf("Oldcoin", "Sylvacoin", "Freecoin"), itemNames(data.at("/leastUsed/items")))
    }

    @Test
    fun `cryptos searches the same way the REST endpoint does`() {
        assertEquals(
            listOf("Sylvacoin"),
            names(graphQL("""query { cryptos(query: "Sylvacon") { items { name } } }""")),
        )
    }

    @Test
    fun `coins load by id and resolve by ticker`() {
        val response = graphQL(
            """
            query {
              cryptosByIds(ids: ["freecoin", "OLDCOIN", "nosuchcoin"]) { name }
              byTicker: crypto(id: "SYL") { name }
              byAlias: crypto(id: "sylvacoin") { name }
              unknown: crypto(id: "nosuchcoin") { name }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/cryptosByIds"), "name", "Freecoin", "Oldcoin")
        assertEquals("Sylvacoin", response.at("/data/byTicker/name").stringValue())
        assertEquals("Sylvacoin", response.at("/data/byAlias/name").stringValue())
        assertTrue(isNullOrMissing(response.at("/data/unknown")))
    }

    private fun names(response: JsonNode): List<String> {
        assertNoErrors(response)

        return itemNames(response.at("/data/cryptos/items"))
    }

    private fun itemNames(items: JsonNode): List<String> {
        val names = mutableListOf<String>()
        for (item in items) {
            names += item["name"].stringValue()
        }

        return names
    }
}
