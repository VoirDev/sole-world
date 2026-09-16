package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the capabilities GraphQL had drifted away from REST on.
 *
 * `cities` took only `page`, so city search was REST-only and a GraphQL caller either paged
 * blindly through 3,008 pages or went via `Country.cities`. `type State` carried no relationships at
 * all, and neither did `type CentralBank`. The rule these tests hold to is that every capability a
 * store has is reachable from both transports.
 */
class TransportParityIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `cities searches and filters like its REST collection`() {
        val response = graphQL(
            """
            query {
              search: cities(query: "North City") { items { name } }
              byCountry: cities(countryId: "1") { pageInfo { totalItems } }
              byState: cities(stateId: "501") { items { name } }
              both: cities(countryId: "1", stateId: "501") { items { name } }
              mismatched: cities(countryId: "2", stateId: "501") { pageInfo { totalItems } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(names(response, "search").contains("North City"))
        assertTrue(response.at("/data/byCountry/pageInfo/totalItems").intValue() > 0)
        assertTrue(names(response, "byState").isNotEmpty())
        assertEquals(names(response, "byState"), names(response, "both"))
        assertEquals(0, response.at("/data/mismatched/pageInfo/totalItems").intValue())
    }

    @Test
    fun `states searches and filters like its REST collection`() {
        val response = graphQL(
            """
            query {
              search: states(query: "North Freedonia") { items { name } }
              byCountry: states(countryId: "1") { items { name } }
              noMatch: states(countryId: "999") { pageInfo { totalItems } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(names(response, "search").isNotEmpty())
        assertTrue(names(response, "byCountry").isNotEmpty())
        assertEquals(0, response.at("/data/noMatch/pageInfo/totalItems").intValue())
    }

    @Test
    fun `a state resolves its country and pages its cities`() {
        val response = graphQL(
            """
            query {
              state(id: "501") {
                name
                country { id name }
                cities(page: { size: 50 }) { items { name } pageInfo { totalItems } }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertEquals("Freedonia", response.at("/data/state/country/name").stringValue())
        assertTrue(response.at("/data/state/cities/pageInfo/totalItems").intValue() > 0)
    }

    @Test
    fun `a city resolves the state and country it belongs to`() {
        val response = graphQL(
            """
            query {
              city(id: "601") { name stateId state { id name } country { id name } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertEquals("501", response.at("/data/city/stateId").stringValue())
        assertEquals("501", response.at("/data/city/state/id").stringValue())
        assertEquals("Freedonia", response.at("/data/city/country/name").stringValue())
    }

    @Test
    fun `a central bank resolves the countries it serves and the currencies it issues`() {
        val response = graphQL(
            """
            query {
              centralBank(id: "401") {
                name
                countries { name }
                currencies { iso3 }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/centralBank/countries"), "name", "Freedonia")
        assertArrayValues(response.at("/data/centralBank/currencies"), "iso3", "FDC")
    }

    @Test
    fun `every list field accepts the search its REST collection accepts`() {
        val response = graphQL(
            """
            query {
              countries:    countries(query: "Freedonia")  { pageInfo { totalItems } }
              currencies:   currencies(query: "Credit")    { pageInfo { totalItems } }
              cryptos:      cryptos(query: "Freecoin")     { pageInfo { totalItems } }
              languages:    languages(query: "Freedonian") { pageInfo { totalItems } }
              regions:      regions(query: "Test Europe")  { pageInfo { totalItems } }
              subregions:   subregions(query: "Test North") { pageInfo { totalItems } }
              centralBanks: centralBanks(query: "Reserve") { pageInfo { totalItems } }
              states:       states(query: "North Freedonia") { pageInfo { totalItems } }
              cities:       cities(query: "North City")   { pageInfo { totalItems } }
              timezones:    timezones(query: "Freedonia Time") { pageInfo { totalItems } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        for (field in response["data"].propertyNames()) {
            assertTrue(
                response.at("/data/$field/pageInfo/totalItems").intValue() > 0,
                "$field found nothing, so its search argument is not wired up",
            )
        }
    }

    private fun names(response: tools.jackson.databind.JsonNode, alias: String): List<String> {
        val names = mutableListOf<String>()
        for (item in response.at("/data/$alias/items")) {
            names += item["name"].stringValue()
        }

        return names
    }
}
