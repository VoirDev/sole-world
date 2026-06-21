package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimezoneQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `timezone query returns scalar data`() {
        val response = graphQL(
            """
            query {
              timezone(id: 301) {
                id
                zoneName
                tzName
                gmtOffset
                gmtOffsetName
                abbreviation
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val timezone = response.at("/data/timezone")
        assertEquals("301", timezone["id"].stringValue())
        assertEquals("Europe/Freedonia", timezone["zoneName"].stringValue())
        assertEquals("Freedonia Time", timezone["tzName"].stringValue())
        assertEquals(3600, timezone["gmtOffset"].intValue())
        assertEquals("UTC+01:00", timezone["gmtOffsetName"].stringValue())
        assertEquals("FDT", timezone["abbreviation"].stringValue())
    }

    @Test
    fun `timezones query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: timezones(ids: [301, 999]) { id zoneName }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "zoneName", "Europe/Freedonia")
    }

    @Test
    fun `missing timezone query returns null`() {
        val response = graphQL(
            """
            query {
              timezone(id: 999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/timezone")))
    }
}
