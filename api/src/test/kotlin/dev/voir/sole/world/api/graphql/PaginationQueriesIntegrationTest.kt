package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class PaginationQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `all list queries support multiple zero based pages`() {
        val listings = listOf(
            "countries",
            "currencies",
            "languages",
            "flags",
            "regions",
            "subregions",
            "timezones",
            "centralBanks",
            "states",
            "cities",
            "mediaAssets",
        )

        listings.forEach { field ->
            val response = graphQL(
                """
                query {
                  first: $field(page: { page: 0, size: 1 }) {
                    items { id }
                    pageInfo {
                      page
                      size
                      totalItems
                      totalPages
                      hasNextPage
                      hasPreviousPage
                    }
                  }
                  second: $field(page: { page: 1, size: 1 }) {
                    items { id }
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

            val first = response.at("/data/first")
            val second = response.at("/data/second")

            assertEquals(1, first.at("/items").size(), "$field first page item count")
            assertEquals(1, second.at("/items").size(), "$field second page item count")
            assertNotEquals(
                first.at("/items/0/id").stringValue(),
                second.at("/items/0/id").stringValue(),
                "$field should return different records on adjacent pages",
            )

            // Page counts are read from the listing rather than hard coded, so that seeding one
            // more record into a fixture collection does not read as a pagination failure.
            val totalItems = first.at("/pageInfo/totalItems").intValue()

            assertEquals(0, first.at("/pageInfo/page").intValue(), "$field first page number")
            assertEquals(1, second.at("/pageInfo/page").intValue(), "$field second page number")
            assertEquals(1, first.at("/pageInfo/size").intValue(), "$field first page size")
            assertEquals(1, second.at("/pageInfo/size").intValue(), "$field second page size")
            assertTrue(totalItems >= 2, "$field needs at least two records to page through")
            assertEquals(
                totalItems,
                second.at("/pageInfo/totalItems").intValue(),
                "$field second total items",
            )
            // One record per page, so a page exists for each record.
            assertEquals(
                totalItems,
                first.at("/pageInfo/totalPages").intValue(),
                "$field first total pages",
            )
            assertEquals(
                totalItems,
                second.at("/pageInfo/totalPages").intValue(),
                "$field second total pages",
            )
            assertTrue(first.at("/pageInfo/hasNextPage").booleanValue(), "$field first has next")
            assertFalse(first.at("/pageInfo/hasPreviousPage").booleanValue(), "$field first has previous")
            assertEquals(
                totalItems > 2,
                second.at("/pageInfo/hasNextPage").booleanValue(),
                "$field second has next",
            )
            assertTrue(second.at("/pageInfo/hasPreviousPage").booleanValue(), "$field second has previous")
        }
    }

    @Test
    fun `country heavy relationships support multiple zero based pages`() {
        val response = graphQL(
            """
            query {
              country(idOrCode: 1) {
                firstStates: states(page: { page: 0, size: 1 }) {
                  items { id name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
                secondStates: states(page: { page: 1, size: 1 }) {
                  items { id name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
                firstCities: cities(page: { page: 0, size: 1 }) {
                  items { id name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
                secondCities: cities(page: { page: 1, size: 1 }) {
                  items { id name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val country = response.at("/data/country")

        assertRelationshipPages(country.at("/firstStates"), country.at("/secondStates"), "states")
        assertRelationshipPages(country.at("/firstCities"), country.at("/secondCities"), "cities")
    }

    private fun assertRelationshipPages(first: JsonNode, second: JsonNode, name: String) {
        assertEquals(1, first.at("/items").size(), "$name first page item count")
        assertEquals(1, second.at("/items").size(), "$name second page item count")
        assertNotEquals(
            first.at("/items/0/id").stringValue(),
            second.at("/items/0/id").stringValue(),
            "$name should return different records on adjacent pages",
        )
        assertEquals(0, first.at("/pageInfo/page").intValue(), "$name first page number")
        assertEquals(1, second.at("/pageInfo/page").intValue(), "$name second page number")
        assertEquals(2, first.at("/pageInfo/totalItems").intValue(), "$name first total items")
        assertEquals(2, second.at("/pageInfo/totalItems").intValue(), "$name second total items")
        assertTrue(first.at("/pageInfo/hasNextPage").booleanValue(), "$name first has next")
        assertFalse(second.at("/pageInfo/hasNextPage").booleanValue(), "$name second has next")
    }
}
