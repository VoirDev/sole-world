package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `validation queries return true for seeded ids and false for missing ids`() {
        val response = graphQL(
            """
            query {
              validCurrency: isValidCurrency(id: 101) { valid }
              invalidCurrency: isValidCurrency(id: 999) { valid }
              validCountry: isValidCountry(id: 1) { valid }
              invalidCountry: isValidCountry(id: 999) { valid }
              validLanguage: isValidLanguage(id: 201) { valid }
              invalidLanguage: isValidLanguage(id: 999) { valid }
              validRegion: isValidRegion(id: 10) { valid }
              invalidRegion: isValidRegion(id: 999) { valid }
              validSubregion: isValidSubregion(id: 20) { valid }
              invalidSubregion: isValidSubregion(id: 999) { valid }
              validTimezone: isValidTimezone(id: 301) { valid }
              invalidTimezone: isValidTimezone(id: 999) { valid }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val data = response["data"]
        assertTrue(data.at("/validCurrency/valid").booleanValue())
        assertFalse(data.at("/invalidCurrency/valid").booleanValue())
        assertTrue(data.at("/validCountry/valid").booleanValue())
        assertFalse(data.at("/invalidCountry/valid").booleanValue())
        assertTrue(data.at("/validLanguage/valid").booleanValue())
        assertFalse(data.at("/invalidLanguage/valid").booleanValue())
        assertTrue(data.at("/validRegion/valid").booleanValue())
        assertFalse(data.at("/invalidRegion/valid").booleanValue())
        assertTrue(data.at("/validSubregion/valid").booleanValue())
        assertFalse(data.at("/invalidSubregion/valid").booleanValue())
        assertTrue(data.at("/validTimezone/valid").booleanValue())
        assertFalse(data.at("/invalidTimezone/valid").booleanValue())
    }
}
