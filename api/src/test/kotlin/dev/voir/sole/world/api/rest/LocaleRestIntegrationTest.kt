package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.integration.BaseRestIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocaleRestIntegrationTest : BaseRestIntegrationTest() {
    @Test
    fun `locales list what accept language can select`() {
        val page = get("/v1/locales")

        assertEquals(1, page.at("/page/totalItems").intValue())
        val locale = page.at("/items/0")
        assertEquals("ru", locale["id"].stringValue())
        assertEquals("ru", locale["languageId"].stringValue())
        assertEquals("Russian", locale["name"].stringValue())
        assertEquals("Русский", locale["nativeName"].stringValue())
    }

    @Test
    fun `a locale name follows the requested language`() {
        assertEquals("Русский", get("/v1/locales/ru?lang=ru")["name"].stringValue())
    }

    @Test
    fun `a locale is found by its tag in any case`() {
        val locale = get("/v1/locales/RU")

        assertEquals("ru", locale["id"].stringValue())
        assertNull(locale["language"], "relationships are only embedded when asked for")
    }

    @Test
    fun `a locale embeds its language on request`() {
        val locale = get("/v1/locales/ru?include=language")

        assertEquals("ru", locale.at("/language/id").stringValue())
        assertEquals("Russian", locale.at("/language/name").stringValue())
    }

    @Test
    fun `an unknown locale is not found`() {
        assertProblem(rest("/v1/locales/sv"), status = 404)
    }

    @Test
    fun `an unknown include is rejected`() {
        assertProblem(rest("/v1/locales?include=countries"), status = 400)
    }
}
