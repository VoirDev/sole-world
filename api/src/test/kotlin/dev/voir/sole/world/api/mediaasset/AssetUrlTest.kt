package dev.voir.sole.world.api.mediaasset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AssetUrlTest {
    @Test
    fun `a dataset path is published rooted at this deployment`() {
        // The regression: the dataset stores "assets/flags/zw_1x1.svg" with no leading slash, so
        // joining it to a base gave "http://hostassets/..." and resolving it against the request
        // path gave "/v1/assets/...". Neither is what the caller wanted.
        val urls = AssetUrl("")

        assertEquals("/assets/flags/zw_1x1.svg", urls.of("assets/flags/zw_1x1.svg"))
        assertEquals("/assets/flags/zw_1x1.svg", urls.of("/assets/flags/zw_1x1.svg"))
    }

    @Test
    fun `a configured base url publishes absolute urls`() {
        val urls = AssetUrl("https://cdn.example.com")

        assertEquals("https://cdn.example.com/assets/flags/zw_1x1.svg", urls.of("assets/flags/zw_1x1.svg"))
        assertEquals("https://cdn.example.com/assets/flags/zw_1x1.svg", urls.of("/assets/flags/zw_1x1.svg"))
    }

    @Test
    fun `a trailing slash on the base url does not double up`() {
        assertEquals(
            "https://cdn.example.com/assets/a.svg",
            AssetUrl("https://cdn.example.com/").of("assets/a.svg"),
        )
    }

    @Test
    fun `a path that is already absolute is left alone`() {
        val urls = AssetUrl("https://cdn.example.com")

        assertEquals("https://other.example/a.svg", urls.of("https://other.example/a.svg"))
        assertEquals("http://other.example/a.svg", urls.of("http://other.example/a.svg"))
    }

    @Test
    fun `a rendition the asset does not have stays absent`() {
        val urls = AssetUrl("https://cdn.example.com")

        assertNull(urls.of(null))
        assertNull(urls.of(""))
        assertNull(urls.of("   "))
    }
}
