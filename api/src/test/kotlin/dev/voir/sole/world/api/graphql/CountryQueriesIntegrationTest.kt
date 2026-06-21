package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CountryQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `country query returns exact nested graph`() {
        val response = graphQL(
            """
            query {
              country(id: 1) {
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
        assertEquals("1", country.at("/id").stringValue())
        assertEquals("Freedonia", country.at("/name").stringValue())
        assertEquals("Test Europe", country.at("/region/name").stringValue())
        assertEquals("Test Europe", country.at("/subregion/region/name").stringValue())
        assertEquals(
            setOf("xs", "sm", "md", "lg", "xl"),
            country.at("/flag/squareAsset/image/formats/png").propertyNames().toSet(),
        )

        assertArrayValues(country["currencies"], "iso3", "FDC")
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
              requested: countries(ids: [2, 1, 999]) {
                id
                name
                region { id name }
                subregion { id name }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "id", "1", "2")
        assertArrayValues(response.at("/data/requested"), "name", "Freedonia", "Sylvania")
    }

    @Test
    fun `listCountries returns default paginated result`() {
        val response = graphQL(
            """
            query {
              listCountries {
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
        val page = response.at("/data/listCountries")
        assertArrayValues(page["items"], "name", "Freedonia", "Sylvania")
        assertEquals(0, page.at("/pageInfo/page").intValue())
        assertEquals(50, page.at("/pageInfo/size").intValue())
        assertEquals(2, page.at("/pageInfo/totalItems").intValue())
        assertEquals(1, page.at("/pageInfo/totalPages").intValue())
        assertFalse(page.at("/pageInfo/hasNextPage").booleanValue())
        assertFalse(page.at("/pageInfo/hasPreviousPage").booleanValue())
    }

    @Test
    fun `country search returns capped results`() {
        val response = graphQL(
            """
            query {
              searchCountries(query: "Freedonia", limit: 10) {
                id
                name
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/searchCountries"), "id", "1")
        assertArrayValues(response.at("/data/searchCountries"), "name", "Freedonia")
    }

    @Test
    fun `plural queries reject more than fifty ids`() {
        val ids = (1..51).joinToString()
        val response = graphQL(
            """
            query {
              countries(ids: [$ids]) { id }
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
              country(id: 1) {
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
        assertArrayValues(country["currencies"], "iso3", "FDC")
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
              country(id: 1) {
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
    fun `missing and invalid country queries return negative results`() {
        val response = graphQL(
            """
            query {
              missingCountry: country(id: 999) { id }
              isValidCountry(id: 1) { valid }
              isInvalidCountry: isValidCountry(id: 999) { valid }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val data = response["data"]
        assertTrue(isNullOrMissing(data["missingCountry"]))
        assertTrue(data.at("/isValidCountry/valid").booleanValue())
        assertFalse(data.at("/isInvalidCountry/valid").booleanValue())
    }

    @Test
    fun `country query requires api access key`() {
        val missingKey = graphQL("{ countries(ids: [1]) { id } }", apiKey = null)
        val invalidKey = graphQL("{ countries(ids: [1]) { id } }", apiKey = "wrong-key")

        assertEquals(1, missingKey["errors"].size())
        assertTrue(
            missingKey.at("/errors/0/message").stringValue().contains("Missing API access key"),
        )
        assertTrue(isNullOrMissing(missingKey.at("/data/countries")))

        assertEquals(1, invalidKey["errors"].size())
        assertTrue(
            invalidKey.at("/errors/0/message").stringValue().contains("Invalid API access key"),
        )
        assertTrue(isNullOrMissing(invalidKey.at("/data/countries")))
    }

    @Test
    fun `country query depth limit rejects over nested documents`() {
        val response = graphQL(
            """
            query {
              country(id: 1) {
                subregion {
                  countries {
                    subregion {
                      countries {
                        subregion {
                          countries {
                            subregion {
                              countries {
                                id
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent(),
        )

        assertTrue(response["errors"].size() > 0)
        assertTrue(
            response.at("/errors/0/message").stringValue()
                .contains("maximum query depth", ignoreCase = true),
        )
        assertTrue(isNullOrMissing(response["data"]))
    }
}
