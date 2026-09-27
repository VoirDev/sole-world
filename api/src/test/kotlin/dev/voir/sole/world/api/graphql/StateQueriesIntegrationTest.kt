package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `state query returns coordinates when available`() {
        val response = graphQL(
            """
            query {
              state(id: "FD-NF") {
                id
                name
                stateCode
                coordinates { latitude longitude }
                type
                countryId
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val state = response.at("/data/state")
        assertEquals("FD-NF", state["id"].stringValue())
        assertEquals("North Freedonia", state["name"].stringValue())
        assertEquals("NF", state["stateCode"].stringValue())
        assertEquals("province", state["type"].stringValue())
        assertEquals("FD", state["countryId"].stringValue())
        assertEquals(45.2, state.at("/coordinates/latitude").doubleValue(), 0.000001)
        assertEquals(19.3, state.at("/coordinates/longitude").doubleValue(), 0.000001)
    }

    @Test
    fun `states query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: statesByIds(ids: ["FD-NF", "fd-sf", "FD-XX"]) { id name coordinates { latitude } }
              stateWithoutCoordinates: state(id: "FD-SF") { id coordinates { latitude } }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "name", "North Freedonia", "South Freedonia")
        assertTrue(isNullOrMissing(response.at("/data/stateWithoutCoordinates/coordinates")))
    }

    @Test
    fun `missing state query returns null`() {
        val response = graphQL(
            """
            query {
              state(id: "FD-XX") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/state")))
    }
}
