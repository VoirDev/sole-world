package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.dataset.json.LocaleJSON
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocaleNegotiationTest {
    /** The bundled locales, in the order `locales.json` lists them. */
    private val negotiation = LocaleNegotiation(
        listOf(
            "de", "es", "fa", "fr", "hr", "it", "ja", "ko", "nl", "pl", "pt", "pt-BR", "ru", "tr", "uk",
            "zh-CN",
        ).map { id -> LocaleJSON(id, id.substringBefore('-'), id, id, emptyList()) },
    )

    @Test
    fun `resolves a supported locale`() {
        assertEquals("ru", negotiation.resolveHeader("ru"))
    }

    @Test
    fun `resolves a regional tag to its canonical spelling`() {
        assertEquals("pt-BR", negotiation.resolveHeader("pt-br"))
    }

    @Test
    fun `falls back from an unsupported region to its language`() {
        assertEquals("de", negotiation.resolveHeader("de-AT"))
    }

    @Test
    fun `a language falls back to the locale spelled as the bare language`() {
        // pt-BR is listed after pt, but either way pt is the one that is simply "Portuguese".
        assertEquals("pt", negotiation.resolveHeader("pt-PT"))
        assertEquals("pt", negotiation.resolveHeader("pt"))
    }

    @Test
    fun `a language without a bare locale falls back to the one it has`() {
        assertEquals("zh-CN", negotiation.resolveHeader("zh"))
        assertEquals("zh-CN", negotiation.resolveHeader("zh-TW"))
        assertEquals("zh-CN", negotiation.resolveHeader("zh-Hans-CN"))
    }

    @Test
    fun `prefers the highest quality range`() {
        assertEquals("ru", negotiation.resolveHeader("de;q=0.2, ru;q=0.9"))
    }

    @Test
    fun `equal qualities keep header order`() {
        assertEquals("de", negotiation.resolveHeader("de, ru"))
    }

    @Test
    fun `ignores ranges the caller explicitly refused`() {
        assertEquals("de", negotiation.resolveHeader("ru;q=0, de"))
    }

    @Test
    fun `english selects base data`() {
        assertNull(negotiation.resolveHeader("en"))
        assertNull(negotiation.resolveHeader("en-US,en;q=0.8,ru;q=0.7"))
    }

    @Test
    fun `wildcard selects base data`() {
        assertNull(negotiation.resolveHeader("*"))
    }

    @Test
    fun `unsupported languages select base data`() {
        assertNull(negotiation.resolveHeader("sv"))
    }

    @Test
    fun `missing and blank headers select base data`() {
        assertNull(negotiation.resolveHeader(null))
        assertNull(negotiation.resolveHeader("   "))
    }

    @Test
    fun `an explicit tag resolves without quality parsing`() {
        assertEquals("uk", negotiation.resolveTag("uk"))
        assertEquals("zh-CN", negotiation.resolveTag("zh-cn"))
        assertNull(negotiation.resolveTag("en"))
        assertNull(negotiation.resolveTag(null))
    }

    @Test
    fun `every supported id is canonical and resolvable`() {
        assertEquals(16, negotiation.supportedIds.size)

        for (id in negotiation.supportedIds) {
            assertEquals(id, negotiation.resolveTag(id), "id $id should round-trip")
        }
    }
}
