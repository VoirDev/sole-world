package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CityQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `city query returns coordinates`() {
        val response = graphQL(
            """
            query {
              city(id: 601) {
                id
                name
                coordinates { latitude longitude }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val city = response.at("/data/city")
        assertEquals("601", city["id"].stringValue())
        assertEquals("North City", city["name"].stringValue())
        assertEquals(45.21, city.at("/coordinates/latitude").doubleValue(), 0.000001)
        assertEquals(19.31, city.at("/coordinates/longitude").doubleValue(), 0.000001)
    }

    @Test
    fun `cities query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: cities(ids: [601, 602, 999]) { id name }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "name", "North City", "South City")
    }

    @Test
    fun `listCities supports zero based pages`() {
        val response = graphQL(
            """
            query {
              listCities(page: { page: 1, size: 1 }) {
                items { id name }
                pageInfo {
                  page
                  size
                  totalItems
                  totalPages
                  hasNextPage
                  hasPreviousPage
                }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val page = response.at("/data/listCities")
        assertEquals(1, page.at("/items").size())
        assertEquals(1, page.at("/pageInfo/page").intValue())
        assertEquals(1, page.at("/pageInfo/size").intValue())
        assertEquals(2, page.at("/pageInfo/totalItems").intValue())
        assertEquals(2, page.at("/pageInfo/totalPages").intValue())
        assertFalse(page.at("/pageInfo/hasNextPage").booleanValue())
        assertTrue(page.at("/pageInfo/hasPreviousPage").booleanValue())
    }

    @Test
    fun `missing city query returns null`() {
        val response = graphQL(
            """
            query {
              city(id: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/city")))
    }
}
