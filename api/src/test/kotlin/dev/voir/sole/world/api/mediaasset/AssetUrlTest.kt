package dev.voir.sole.world.api.mediaasset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AssetUrlTest {
    @Test
    fun `a dataset path is published under the route this deployment serves it on`() {
        // The dataset stores "flags/zw_1x1.svg" with no leading slash, so joining it to a base gave
        // "http://hostflags/..." and resolving it against the request path gave a path under /v1.
        val urls = AssetUrl("")

        assertEquals("/assets/flags/zw_1x1.svg", urls.of("flags/zw_1x1.svg"))
        assertEquals("/assets/flags/zw_1x1.svg", urls.of("/flags/zw_1x1.svg"))
    }

    @Test
    fun `a configured base url publishes absolute urls without the assets route`() {
        // The bucket behind the CDN holds flags/ and cryptos/ at its root.
        val urls = AssetUrl("https://cdn.example.com")

        assertEquals("https://cdn.example.com/flags/zw_1x1.svg", urls.of("flags/zw_1x1.svg"))
        assertEquals("https://cdn.example.com/flags/zw_1x1.svg", urls.of("/flags/zw_1x1.svg"))
    }

    @Test
    fun `a trailing slash on the base url does not double up`() {
        assertEquals(
            "https://cdn.example.com/flags/a.svg",
            AssetUrl("https://cdn.example.com/").of("flags/a.svg"),
        )
    }

    @Test
    fun `a blank base url publishes under this deployment`() {
        assertEquals("/assets/flags/a.svg", AssetUrl("   ").of("flags/a.svg"))
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
