package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.ApiAccessKey
import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class CountryQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `country query returns exact nested graph`() {
        val response = graphQL(
            """
            query {
              country(id: "FD") {
                id
                name
                nativeName
                iso3
                iso2
                isoNumeric
                phoneCode
                tld
                coordinates { latitude longitude }
                region { id name }
                subregion { id name region { id name } }
                flag {
                  id
                  caption
                  emoji
                  emojiU
                  squareAsset {
                    id
                    type
                    image {
                      aspectRatio
                      formats {
                        svg
                        png { xs sm md lg xl }
                        webp { xs sm md lg xl }
                        jpg { xs sm md lg xl }
                      }
                    }
                  }
                }
                currencies { id iso3 isoNumeric name decimalDigits obsolete symbol }
                timezones { id zoneName tzName gmtOffset gmtOffsetName abbreviation }
                languages { id code name nativeName }
                centralBanks { id name nativeName websiteUrl establishmentYear }
                states(page: { page: 0, size: 1 }) {
                  items { id name stateCode type coordinates { latitude longitude } countryId }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
                cities(page: { page: 0, size: 1 }) {
                  items { id name coordinates { latitude longitude } }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val country = response.at("/data/country")
        assertEquals(
            setOf(
                "id",
                "name",
                "nativeName",
                "iso3",
                "iso2",
                "isoNumeric",
                "phoneCode",
                "tld",
                "coordinates",
                "region",
                "subregion",
                "flag",
                "currencies",
                "timezones",
                "languages",
                "centralBanks",
                "states",
                "cities",
            ),
            country.propertyNames().toSet(),
        )
        assertEquals("FD", country.at("/id").stringValue())
        assertEquals("Freedonia", country.at("/name").stringValue())
        assertEquals("Test Europe", country.at("/region/name").stringValue())
        assertEquals("Test Europe", country.at("/subregion/region/name").stringValue())
        assertEquals(
            setOf("xs", "sm", "md", "lg", "xl"),
            country.at("/flag/squareAsset/image/formats/png").propertyNames().toSet(),
        )

        assertArrayValues(country["currencies"], "iso3", "FDC", "ALT")
        assertArrayValues(country["timezones"], "zoneName", "Europe/Freedonia")
        assertArrayValues(country["languages"], "code", "fd")
        assertArrayValues(country["centralBanks"], "name", "Freedonian Reserve")
        assertArrayValues(country.at("/states/items"), "name", "North Freedonia")
        assertArrayValues(country.at("/cities/items"), "name", "North City")
        assertEquals(2, country.at("/states/pageInfo/totalItems").intValue())
        assertEquals(2, country.at("/cities/pageInfo/totalItems").intValue())
        assertTrue(country.at("/states/pageInfo/hasNextPage").booleanValue())
        assertTrue(country.at("/cities/pageInfo/hasNextPage").booleanValue())
    }

    @Test
    fun `countries query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: countriesByIds(ids: ["SY", "fd", "XX"]) {
                id
                name
                region { id name }
                subregion { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "id", "FD", "SY")
        assertArrayValues(response.at("/data/requested"), "name", "Freedonia", "Sylvania")
    }

    @Test
    fun `countries returns default paginated result`() {
        val response = graphQL(
            """
            query {
              countries {
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
        val page = response.at("/data/countries")
        assertArrayValues(page["items"], "name", "Freedonia", "Sylvania")
        assertEquals(0, page.at("/pageInfo/page").intValue())
        assertEquals(50, page.at("/pageInfo/size").intValue())
        assertEquals(2, page.at("/pageInfo/totalItems").intValue())
        assertEquals(1, page.at("/pageInfo/totalPages").intValue())
        assertFalse(page.at("/pageInfo/hasNextPage").booleanValue())
        assertFalse(page.at("/pageInfo/hasPreviousPage").booleanValue())
    }

    @Test
    fun `countries searches and filters the way the REST collection does`() {
        val response = graphQL(
            """
            query {
              search: countries(query: "Freedonia") { items { id name } }
              byRegion: countries(regionId: "test-europe") { items { name } }
              byCurrency: countries(currencyId: "FDC") { items { name } }
              byLanguage: countries(languageId: "FD") { items { name } }
              byTimezone: countries(timezoneId: "europe/freedonia") { items { name } }
              noMatch: countries(regionId: "atlantis") { pageInfo { totalItems } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/search/items"), "id", "FD")
        assertArrayValues(response.at("/data/search/items"), "name", "Freedonia")
        assertArrayValues(response.at("/data/byRegion/items"), "name", "Freedonia", "Sylvania")
        assertArrayValues(response.at("/data/byCurrency/items"), "name", "Freedonia")
        assertArrayValues(response.at("/data/byLanguage/items"), "name", "Freedonia")
        assertArrayValues(response.at("/data/byTimezone/items"), "name", "Freedonia")
        assertEquals(0, response.at("/data/noMatch/pageInfo/totalItems").intValue())
    }

    @Test
    fun `plural queries reject more than fifty ids`() {
        val ids = (1..51).joinToString()
        val response = graphQL(
            """
            query {
              countriesByIds(ids: [$ids]) { id }
            }
            """.trimIndent(),
        )

        assertTrue(response["errors"].size() > 0)
        assertTrue(
            response.at("/errors/0/message").stringValue()
                .contains("ids cannot contain more than 50 values"),
        )
    }

    @Test
    fun `country relationships do not leak unrelated records`() {
        val response = graphQL(
            """
            query {
              country(id: "FD") {
                currencies { iso3 }
                languages { code }
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
        )

        assertNoErrors(response)
        val country = response.at("/data/country")
        assertArrayValues(country["currencies"], "iso3", "FDC", "ALT")
        assertArrayValues(country["languages"], "code", "fd")
        assertArrayValues(country.at("/states/items"), "name", "North Freedonia", "South Freedonia")
        assertArrayValues(country.at("/cities/items"), "name", "North City", "South City")
        assertEquals(2, country.at("/states/pageInfo/totalItems").intValue())
        assertEquals(2, country.at("/cities/pageInfo/totalItems").intValue())
    }

    @Test
    fun `country heavy relationships support pagination and name filters`() {
        val response = graphQL(
            """
            query {
              country(id: "FD") {
                filteredStates: states(query: "South", page: { page: 0, size: 1 }) {
                  items { name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
                filteredCities: cities(query: "South", page: { page: 0, size: 1 }) {
                  items { name }
                  pageInfo { page size totalItems totalPages hasNextPage hasPreviousPage }
                }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val country = response.at("/data/country")
        assertArrayValues(country.at("/filteredStates/items"), "name", "South Freedonia")
        assertArrayValues(country.at("/filteredCities/items"), "name", "South City")
        assertEquals(1, country.at("/filteredStates/pageInfo/totalItems").intValue())
        assertEquals(1, country.at("/filteredCities/pageInfo/totalItems").intValue())
        assertFalse(country.at("/filteredStates/pageInfo/hasNextPage").booleanValue())
        assertFalse(country.at("/filteredCities/pageInfo/hasNextPage").booleanValue())
    }

    @Test
    fun `a country resolves by its alpha-2 id and by its alpha-3 and numeric codes`() {
        val response = graphQL(
            """
            query {
              byId: country(id: "FD") { id }
              byLowercaseId: country(id: "fd") { id }
              byIso3: country(id: "FRE") { id }
              byNumeric: country(id: "901") { id }
              missing: country(id: "XX") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val data = response["data"]
        assertEquals("FD", data.at("/byId/id").stringValue())
        assertEquals("FD", data.at("/byLowercaseId/id").stringValue())
        assertEquals("FD", data.at("/byIso3/id").stringValue())
        assertEquals("FD", data.at("/byNumeric/id").stringValue())
        assertTrue(isNullOrMissing(data["missing"]))
    }

    @Test
    fun `country query requires api access key`() {
        val query = "{ countriesByIds(ids: [\"FD\"]) { id } }"

        val missingKey = graphQLResponse(query, apiKey = null)
        val invalidKey = graphQLResponse(query, apiKey = "wrong-key")

        // Authentication is rejected at the transport, before GraphQL executes.
        assertEquals(401, missingKey.statusCode())
        assertEquals(401, invalidKey.statusCode())

        assertTrue(
            missingKey.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"),
        )
        assertTrue(missingKey.body().contains("\"status\":401"))
    }

    @Test
    fun `country query accepts the api key as a bearer token`() {
        val response = graphQLResponse(
            "{ countriesByIds(ids: [\"FD\"]) { id } }",
            apiKey = null,
            bearerToken = ApiAccessKey.RAW,
        )

        assertEquals(200, response.statusCode())
    }

    @Test
    fun `country aliases follow the requested language`() {
        val query = """query { country(id: "FD") { aliases } }"""

        assertEquals(
            listOf("Free Republic", "F.R.D."),
            strings(graphQL(query).at("/data/country/aliases")),
        )
        assertEquals(
            listOf("Фридонская Республика"),
            strings(graphQL(query, acceptLanguage = "ru").at("/data/country/aliases")),
        )
    }

    private fun strings(array: JsonNode): List<String> = array.asSequence().map { it.stringValue() }.toList()
}
