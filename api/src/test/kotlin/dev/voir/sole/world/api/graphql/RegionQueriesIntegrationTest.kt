package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegionQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `region query returns subregions and countries`() {
        val response = graphQL(
            """
            query {
              region(id: "test-europe") {
                id
                name
                subregions { id name regionId }
                countries { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val region = response.at("/data/region")
        assertEquals("test-europe", region["id"].stringValue())
        assertEquals("Test Europe", region["name"].stringValue())
        assertArrayValues(region["subregions"], "name", "Test North", "Test South")
        assertArrayValues(region["countries"], "name", "Freedonia", "Sylvania")
    }

    @Test
    fun `regions query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: regionsByIds(ids: ["test-europe", "atlantis"]) { id name }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "name", "Test Europe")
    }

    @Test
    fun `missing region query returns null`() {
        val response = graphQL(
            """
            query {
              region(id: "atlantis") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/region")))
    }
}
