package dev.voir.sole.world.api.dataset.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextIndexTest {
    @Test
    fun `normalization folds case`() {
        assertEquals("germany", TextIndex.normalize("GerMANY"))
    }

    @Test
    fun `normalization strips accents`() {
        assertEquals("cote divoire", TextIndex.normalize("Côte d’Ivoire"))
        assertEquals("espana", TextIndex.normalize("España"))
    }

    @Test
    fun `normalization reduces punctuation to word boundaries`() {
        assertEquals("usa", TextIndex.normalize("U.S.A."))
        assertEquals("usa", TextIndex.normalize("U. S. A."))
        assertEquals("guinea bissau", TextIndex.normalize("Guinea-Bissau"))
        assertEquals("washington dc", TextIndex.normalize("Washington, D.C."))
        // Straight and curly apostrophes, and the Ukrainian modifier letter, all fold the same way.
        assertEquals(TextIndex.normalize("Côte d'Ivoire"), TextIndex.normalize("Côte d’Ivoire"))
        assertEquals("мянма", TextIndex.normalize("Мʼянма"))
    }

    @Test
    fun `single letters close up only when they run together`() {
        assertEquals("trinidad y tobago", TextIndex.normalize("Trinidad y Tobago"))
        assertEquals("us of a", TextIndex.normalize("U.S. of A"))
    }

    @Test
    fun `text that is only punctuation keeps it`() {
        assertEquals("-", TextIndex.normalize("-"))
        assertNotNull(SearchQuery.of("-"))
    }

    @Test
    fun `normalization folds full width forms`() {
        assertEquals("japan", TextIndex.normalize("Ｊａｐａｎ"))
    }

    @Test
    fun `normalization lowercases cyrillic`() {
        // The long-standing "cyrillic search" gap: the query and the indexed name were folded
        // differently, so a Russian query could never match a Russian name.
        assertEquals("россия", TextIndex.normalize("Россия"))
    }

    @Test
    fun `cyrillic queries match cyrillic names`() {
        val query = query("РОССИЯ")

        assertTrue(query.matches("Россия"))
        assertTrue(query.score("Россия", 100) > 0)
    }

    @Test
    fun `accented queries match unaccented names and the reverse`() {
        assertTrue(query("cote").matches("Côte d'Ivoire"))
        assertTrue(query("CÔTE").matches("Cote d'Ivoire"))
    }

    @Test
    fun `exact matches outrank prefixes which outrank substrings`() {
        val query = query("land")

        val exact = query.score("Land", 100)
        val prefix = query.score("Landia", 100)
        val substring = query.score("Finland", 100)

        assertTrue(exact > prefix, "exact $exact should beat prefix $prefix")
        assertTrue(prefix > substring, "prefix $prefix should beat substring $substring")
        assertTrue(substring > 0)
    }

    @Test
    fun `field weight orders matches of the same kind`() {
        val query = query("fr")

        assertTrue(query.score("fr", 100) > query.score("fr", 50))
    }

    @Test
    fun `trigram similarity ranks a typo below every literal match`() {
        val query = query("Germny")

        val fuzzy = query.score("Germany", 100)
        val literal = query.score("Germny Republic", 100)

        assertTrue(fuzzy > 0, "a one-character typo should still match")
        assertTrue(fuzzy < literal, "fuzzy $fuzzy must rank below literal $literal")
    }

    @Test
    fun `unrelated names do not match`() {
        val query = query("South")

        assertEquals(0, query.score("North Freedonia", 100))
        assertFalse(query.matches("North Freedonia"))
    }

    @Test
    fun `short queries are not scored fuzzily`() {
        // Below the trigram threshold every three-letter code would look similar to every other.
        val query = query("fra")

        assertEquals(0, query.score("deu", 100))
    }

    @Test
    fun `filters stay literal so result counts remain trustworthy`() {
        val query = query("Germny")

        assertTrue(query.score("Germany", 100) > 0)
        assertFalse(query.matches("Germany"))
    }

    @Test
    fun `blank queries are rejected`() {
        assertNull(SearchQuery.of(""))
        assertNull(SearchQuery.of("   "))
        assertNotNull(SearchQuery.of(" fr "))
    }

    @Test
    fun `identical strings are perfectly similar`() {
        val trigrams = TextIndex.trigrams(TextIndex.normalize("germany"))

        assertEquals(1.0, TextIndex.similarity(trigrams, "germany"))
    }

    private fun query(raw: String): SearchQuery = requireNotNull(SearchQuery.of(raw))
}

/** Folds raw text at the call site, the way a store folds a name when it is built. */
private fun SearchQuery.matches(raw: String): Boolean = matches(FoldedText.of(raw))
