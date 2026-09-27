package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `flag query returns media asset nestings`() {
        val response = graphQL(
            """
            query {
              flag(id: "fd") {
                id
                caption
                emoji
                emojiU
                squareAssetId
                wideAssetId
                squareAsset {
                  id
                  type
                  image { aspectRatio formats { svg png { xs sm md lg xl } } }
                }
                wideAsset {
                  id
                  type
                  image { aspectRatio formats { svg png { xs sm md lg xl } webp { xs } jpg { xs } } }
                }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val flag = response.at("/data/flag")
        assertEquals("fd", flag["id"].stringValue())
        assertEquals("Freedonia flag", flag["caption"].stringValue())
        assertEquals("flag-fd-square", flag["squareAssetId"].stringValue())
        assertEquals("flag-fd-wide", flag["wideAssetId"].stringValue())
        assertEquals("Square", flag.at("/squareAsset/image/aspectRatio").stringValue())
        assertEquals("Wide", flag.at("/wideAsset/image/aspectRatio").stringValue())
        assertEquals(
            setOf("xs", "sm", "md", "lg", "xl"),
            flag.at("/squareAsset/image/formats/png").propertyNames().toSet(),
        )
    }

    @Test
    fun `flags query supports required ids`() {
        val response = graphQL(
            """
            query {
              requested: flagsByIds(ids: ["fd", "xx"]) { id caption }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertArrayValues(response.at("/data/requested"), "caption", "Freedonia flag")
    }

    @Test
    fun `missing flag query returns null`() {
        val response = graphQL(
            """
            query {
              flag(id: "xx") { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/flag")))
    }
}
