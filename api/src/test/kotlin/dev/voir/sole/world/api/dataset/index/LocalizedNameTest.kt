package dev.voir.sole.world.api.dataset.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalizedNameTest {
    private val name = LocalizedName.of(
        base = "United States",
        translations = listOf("ru" to "Соединенные Штаты", "de" to "Vereinigte Staaten"),
        baseAliases = listOf("USA", "America"),
        translatedAliases = listOf("ru" to listOf("США", "Америка")),
    )

    @Test
    fun `aliases resolve in the language of the name`() {
        assertEquals(listOf("USA", "America"), name.aliases(null))
        assertEquals(listOf("США", "Америка"), name.aliases("ru"))
    }

    @Test
    fun `a translated name without aliases has none rather than english ones`() {
        assertEquals(emptyList<String>(), name.aliases("de"))
    }

    @Test
    fun `aliases fall back to english together with the name`() {
        assertEquals("United States", name.resolve("fr"))
        assertEquals(listOf("USA", "America"), name.aliases("fr"))
    }

    @Test
    fun `every alias is searchable whatever the caller reads`() {
        val query = SearchQuery.of("сша")!!

        assertEquals(0, name.score(query, 100))
        assertEquals(98, name.scoreAliases(query, 98))
        assertTrue(name.matches(query))
    }

    @Test
    fun `a record without aliases scores nothing for them`() {
        val plain = LocalizedName.of(base = "Sylvania", translations = emptyList())

        assertEquals(0, plain.scoreAliases(SearchQuery.of("sylvania")!!, 98))
        assertEquals(emptyList<String>(), plain.aliases(null))
    }
}
