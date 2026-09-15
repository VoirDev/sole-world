package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubregionQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `subregion query returns region and countries`() {
        val response = graphQL(
            """
            query {
              subregion(id: 20) {
                id
                name
                regionId
                region { id name }
                countries { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val subregion = response.at("/data/subregion")
        assertEquals("20", subregion["id"].stringValue())
        assertEquals("Test North", subregion["name"].stringValue())
        assertEquals("Test Europe", subregion.at("/region/name").stringValue())
        assertArrayValues(subregion["countries"], "name", "Freedonia")
    }

    @Test
    fun `subregions query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: subregionsByIds(ids: [20, 999]) { id name regionId }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "name", "Test North")
    }

    @Test
    fun `missing subregion query returns null`() {
        val response = graphQL(
            """
            query {
              subregion(id: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/subregion")))
    }
}
