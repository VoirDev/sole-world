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
              country:     country(id: "FD")                    { id }
              currency:    currency(id: "FDC")                  { id }
              crypto:      crypto(id: "freecoin")               { id }
              language:    language(id: "fd")                   { id }
              region:      region(id: "test-europe")            { id }
              subregion:   subregion(id: "test-north")          { id }
              centralBank: centralBank(id: "freedonian-reserve") { id }
              state:       state(id: "FD-NF")                   { id }
              city:        city(id: "601")                      { id }
              timezone:    timezone(id: "Europe/Freedonia")     { id }
              flag:        flag(id: "fd")                       { id }
              mediaAsset:  mediaAsset(id: "flag-fd-square")     { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        for ((field, id) in EXISTING) {
            assertEquals(id, response.at("/data/$field/id").stringValue(), field)
        }
    }

    @Test
    fun `a lookup ignores case and answers with the id as published`() {
        // A caller that stored or typed an id in another case still finds the record, and learns
        // the one spelling every response uses.
        val response = graphQL(
            """
            query {
              country:     country(id: "fd")                    { id }
              currency:    currency(id: "fdc")                  { id }
              crypto:      crypto(id: "FREECOIN")               { id }
              language:    language(id: "FD")                   { id }
              region:      region(id: "TEST-EUROPE")            { id }
              subregion:   subregion(id: "Test-North")          { id }
              centralBank: centralBank(id: "Freedonian-Reserve") { id }
              state:       state(id: "fd-nf")                   { id }
              city:        city(id: "601")                      { id }
              timezone:    timezone(id: "europe/freedonia")     { id }
              flag:        flag(id: "FD")                       { id }
              mediaAsset:  mediaAsset(id: "FLAG-FD-SQUARE")     { id }
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
              country:     country(id: "XX")         { id }
              currency:    currency(id: "XXX")       { id }
              crypto:      crypto(id: "nosuchcoin")  { id }
              language:    language(id: "xx")        { id }
              region:      region(id: "atlantis")    { id }
              subregion:   subregion(id: "atlantis") { id }
              centralBank: centralBank(id: "none")   { id }
              state:       state(id: "FD-XX")        { id }
              city:        city(id: "999")           { id }
              timezone:    timezone(id: "Mars/Olympus") { id }
              flag:        flag(id: "xx")            { id }
              mediaAsset:  mediaAsset(id: "flag-xx-square") { id }
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
            "country" to "FD",
            "currency" to "FDC",
            "crypto" to "freecoin",
            "language" to "fd",
            "region" to "test-europe",
            "subregion" to "test-north",
            "centralBank" to "freedonian-reserve",
            "state" to "FD-NF",
            "city" to "601",
            "timezone" to "Europe/Freedonia",
            "flag" to "fd",
            "mediaAsset" to "flag-fd-square",
        )
    }
}
