package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers asking whether a record exists.
 *
 * The schema used to carry `isValidX` fields for six of the eleven entities, which answered exactly
 * what the nullable single-item lookup already answers — and only for some of them, so a caller
 * asking about a city or a flag had to use the lookup anyway. Selecting `id` from the lookup is the
 * one way that works for every entity.
 */
class ExistenceQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `a lookup answers existence for every entity, the same way`() {
        val response = graphQL(
            """
            query {
              country:     country(idOrCode: "1")    { id }
              currency:    currency(idOrCode: "101")  { id }
              crypto:      crypto(idOrCode: "1")      { id }
              language:    language(idOrCode: "201")  { id }
              region:      region(id: "10")           { id }
              subregion:   subregion(id: "20")        { id }
              centralBank: centralBank(id: "401")     { id }
              state:       state(id: "501")           { id }
              city:        city(id: "601")            { id }
              timezone:    timezone(id: "301")        { id }
              flag:        flag(id: "1001")           { id }
              mediaAsset:  mediaAsset(id: "9001")     { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        for ((field, id) in EXISTING) {
            assertEquals(id, response.at("/data/$field/id").stringValue(), field)
        }
    }

    @Test
    fun `a lookup for a record that does not exist is null rather than an error`() {
        val response = graphQL(
            """
            query {
              country:     country(idOrCode: "999")  { id }
              currency:    currency(idOrCode: "999") { id }
              crypto:      crypto(idOrCode: "999")   { id }
              language:    language(idOrCode: "999") { id }
              region:      region(id: "999")         { id }
              subregion:   subregion(id: "999")      { id }
              centralBank: centralBank(id: "999")    { id }
              state:       state(id: "999")          { id }
              city:        city(id: "999")           { id }
              timezone:    timezone(id: "999")       { id }
              flag:        flag(id: "999")           { id }
              mediaAsset:  mediaAsset(id: "999")     { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        for ((field, _) in EXISTING) {
            assertTrue(isNullOrMissing(response.at("/data/$field")), "$field should be null")
        }
    }

    private companion object {
        val EXISTING = listOf(
            "country" to "1",
            "currency" to "101",
            "crypto" to "1",
            "language" to "201",
            "region" to "10",
            "subregion" to "20",
            "centralBank" to "401",
            "state" to "501",
            "city" to "601",
            "timezone" to "301",
            "flag" to "1001",
            "mediaAsset" to "9001",
        )
    }
}
