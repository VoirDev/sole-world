package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalizationQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `accept language header localizes direct and nested country graph fields`() {
        val response = graphQL(
            """
            query {
              country(id: 1) {
                name
                region { name }
                subregion { name }
                currencies { name description }
                languages { name description }
                timezones { tzName }
                centralBanks { name }
                states {
                  items { name }
                  pageInfo { totalItems }
                }
                cities {
                  items { name }
                  pageInfo { totalItems }
                }
              }
            }
            """.trimIndent(),
            acceptLanguage = "ru",
        )

        assertNoErrors(response)
        val country = response.at("/data/country")
        assertEquals("Фридония", country.at("/name").stringValue())
        assertEquals("Тестовая Европа", country.at("/region/name").stringValue())
        assertEquals("Тестовый Север", country.at("/subregion/name").stringValue())
        assertArrayValues(country["currencies"], "name", "Фридонский кредит")
        assertArrayValues(country["currencies"], "description", "Активная тестовая валюта")
        assertArrayValues(country["languages"], "name", "Фридонский")
        assertArrayValues(country["languages"], "description", "Тестовый официальный язык")
        assertArrayValues(country["timezones"], "tzName", "Фридонское время")
        assertArrayValues(country["centralBanks"], "name", "Фридонский резерв")
        assertArrayValues(country.at("/states/items"), "name", "Северная Фридония", "Южная Фридония")
        assertArrayValues(country.at("/cities/items"), "name", "Северный город", "Южный город")
        assertEquals(2, country.at("/states/pageInfo/totalItems").intValue())
        assertEquals(2, country.at("/cities/pageInfo/totalItems").intValue())
    }

    @Test
    fun `accept language header supports quality fallback and localized list ordering`() {
        val response = graphQL(
            """
            query {
              listCountries(page: { page: 0, size: 1 }) {
                items { name }
                pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
              }
            }
            """.trimIndent(),
            acceptLanguage = "de;q=0.2, ru;q=0.9",
        )

        assertNoErrors(response)
        val page = response.at("/data/listCountries")
        assertArrayValues(page["items"], "name", "Фридония")
        assertEquals(0, page.at("/pageInfo/page").intValue())
        assertEquals(1, page.at("/pageInfo/size").intValue())
        assertEquals(2, page.at("/pageInfo/totalItems").intValue())
        assertEquals(2, page.at("/pageInfo/totalPages").intValue())
    }

    @Test
    fun `english accept language header uses base strings`() {
        val response = graphQL(
            """
            query {
              country(id: 1) {
                name
                region { name }
                states { items { name } }
                cities { items { name } }
              }
            }
            """.trimIndent(),
            acceptLanguage = "en-US,en;q=0.8,ru;q=0.7",
        )

        assertNoErrors(response)
        val country = response.at("/data/country")
        assertEquals("Freedonia", country.at("/name").stringValue())
        assertEquals("Test Europe", country.at("/region/name").stringValue())
        assertArrayValues(country.at("/states/items"), "name", "North Freedonia", "South Freedonia")
        assertArrayValues(country.at("/cities/items"), "name", "North City", "South City")
    }
}
