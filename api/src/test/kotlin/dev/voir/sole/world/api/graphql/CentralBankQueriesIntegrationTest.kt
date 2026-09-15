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
              centralBank(id: 401) {
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
        assertEquals("401", centralBank["id"].stringValue())
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
              requested: centralBanksByIds(ids: [401, 999]) { id name }
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
              centralBank(id: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/centralBank")))
    }
}
