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
                obsolete obsoleteAt logoId
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
    fun `coins are listed by name and can be filtered by obsolescence`() {
        assertEquals(
            listOf("Freecoin", "Oldcoin", "Sylvacoin"),
            names(graphQL("query { cryptos { items { name } } }")),
        )
        assertEquals(
            listOf("Oldcoin"),
            names(graphQL("query { cryptos(obsolete: true) { items { name } } }")),
        )
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

        val names = mutableListOf<String>()
        for (item in response.at("/data/cryptos/items")) {
            names += item["name"].stringValue()
        }

        return names
    }
}
