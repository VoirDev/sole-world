package dev.voir.sole.world.api.dataset.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanguageNegotiationTest {
    @Test
    fun `resolves a supported language`() {
        assertEquals("ru", LanguageNegotiation.resolveHeader("ru"))
    }

    @Test
    fun `resolves a regional tag to its canonical code`() {
        assertEquals("pt-BR", LanguageNegotiation.resolveHeader("pt-BR"))
    }

    @Test
    fun `falls back from an unsupported region to its primary language`() {
        assertEquals("de", LanguageNegotiation.resolveHeader("de-AT"))
    }

    @Test
    fun `prefers the highest quality range`() {
        assertEquals("ru", LanguageNegotiation.resolveHeader("de;q=0.2, ru;q=0.9"))
    }

    @Test
    fun `equal qualities keep header order`() {
        assertEquals("de", LanguageNegotiation.resolveHeader("de, ru"))
    }

    @Test
    fun `ignores ranges the caller explicitly refused`() {
        assertEquals("de", LanguageNegotiation.resolveHeader("ru;q=0, de"))
    }

    @Test
    fun `english selects base data`() {
        assertNull(LanguageNegotiation.resolveHeader("en"))
        assertNull(LanguageNegotiation.resolveHeader("en-US,en;q=0.8,ru;q=0.7"))
    }

    @Test
    fun `wildcard selects base data`() {
        assertNull(LanguageNegotiation.resolveHeader("*"))
    }

    @Test
    fun `unsupported languages select base data`() {
        assertNull(LanguageNegotiation.resolveHeader("sv"))
    }

    @Test
    fun `missing and blank headers select base data`() {
        assertNull(LanguageNegotiation.resolveHeader(null))
        assertNull(LanguageNegotiation.resolveHeader("   "))
    }

    @Test
    fun `an explicit tag resolves without quality parsing`() {
        assertEquals("uk", LanguageNegotiation.resolveTag("uk"))
        assertEquals("zh-CN", LanguageNegotiation.resolveTag("zh-cn"))
        assertNull(LanguageNegotiation.resolveTag("en"))
        assertNull(LanguageNegotiation.resolveTag(null))
    }

    @Test
    fun `every supported code is canonical and resolvable`() {
        assertTrue(LanguageNegotiation.supportedCodes.isNotEmpty())

        for (code in LanguageNegotiation.supportedCodes) {
            assertEquals(code, LanguageNegotiation.resolveTag(code), "code $code should round-trip")
        }
    }
}
