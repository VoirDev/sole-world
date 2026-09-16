package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.api.integration.BaseGraphqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaAssetQueriesIntegrationTest : BaseGraphqlIntegrationTest() {
    @Test
    fun `mediaAsset query returns image formats`() {
        val response = graphQL(
            """
            query {
              mediaAsset(id: 9001) {
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
            """.trimIndent(),
        )

        assertNoErrors(response)
        val asset = response.at("/data/mediaAsset")
        assertEquals("9001", asset["id"].stringValue())
        assertEquals("Image", asset["type"].stringValue())
        assertEquals("Square", asset.at("/image/aspectRatio").stringValue())
        assertEquals("/assets/flags/fd_1x1.svg", asset.at("/image/formats/svg").stringValue())
        assertEquals(
            setOf("xs", "sm", "md", "lg", "xl"),
            asset.at("/image/formats/png").propertyNames().toSet(),
        )
    }

    @Test
    fun `mediaAssets query returns only requested existing assets`() {
        val response = graphQL(
            """
            query {
              mediaAssetsByIds(ids: [9001, 9002, 9999]) {
                id
                type
                image { aspectRatio }
              }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        val assets = response.at("/data/mediaAssetsByIds")
        assertArrayValues(assets, "id", "9001", "9002")
        assertArrayValues(assets, "type", "Image", "Image")
    }

    @Test
    fun `missing mediaAsset query returns null`() {
        val response = graphQL(
            """
            query {
              mediaAsset(id: 9999) { id }
            }
            """.trimIndent(),
        )

        assertNoErrors(response)
        assertTrue(isNullOrMissing(response.at("/data/mediaAsset")))
    }
}
