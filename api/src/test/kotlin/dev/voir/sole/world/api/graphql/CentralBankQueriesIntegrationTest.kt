package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CentralBankQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `centralBank query returns scalar data`() {
        val response = graphQL(
            """
            query {
              centralBank(id: "freedonian-reserve") {
                id
                name
                nativeName
                websiteUrl
                establishmentYear
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val centralBank = response.at("/data/centralBank")
        assertEquals("freedonian-reserve", centralBank["id"].stringValue())
        assertEquals("Freedonian Reserve", centralBank["name"].stringValue())
        assertEquals("Reserve Native", centralBank["nativeName"].stringValue())
        assertEquals("https://bank.example.test", centralBank["websiteUrl"].stringValue())
        assertEquals(1950, centralBank["establishmentYear"].intValue())
    }

    @Test
    fun `centralBanks query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: centralBanksByIds(ids: ["freedonian-reserve", "none"]) { id name }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "name", "Freedonian Reserve")
    }

    @Test
    fun `missing centralBank query returns null`() {
        val response = graphQL(
            """
            query {
              centralBank(id: "none") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/centralBank")))
    }
}
