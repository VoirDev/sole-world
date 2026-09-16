package dev.voir.sole.world.api.dataset.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class LocalizedIndexTest {
    private val records = listOf(
        Record(id = 1, base = "Zurich", translations = mapOf("de" to "Aachen")),
        Record(id = 2, base = "Amsterdam"),
        Record(id = 3, base = "Berlin"),
        Record(id = 4, base = "Zzz", translations = mapOf("de" to "Ärgerlich")),
    )

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name(language) },
        tieBreaker = Record::id,
    )

    @Test
    fun `records are ordered by the name the caller sees, not by the base name`() {
        assertEquals(listOf(2L, 3L, 1L, 4L), index.sorted(null).map { it.id })

        // In German the first record renders as "Aachen" and the last as "Ärgerlich", so both move —
        // and the umlaut sorts where a German reader expects it, between Amsterdam and Berlin.
        assertEquals(listOf(1L, 2L, 4L, 3L), index.sorted("de").map { it.id })
    }

    @Test
    fun `an ordering is built once per language and then reused`() {
        assertSame(index.sorted(null), index.sorted(null))
        assertSame(index.sorted("de"), index.sorted("de"))
        assertNotSame(index.sorted(null), index.sorted("de"))
    }

    @Test
    fun `an unfiltered subset is answered from the cached ordering`() {
        // The regression this guards: city and state list requests passed the full record set to
        // sortSubset, so every request re-sorted the whole dataset instead of using the cache.
        assertSame(index.sorted(null), index.sortSubset(records, null))
        assertSame(index.sorted("de"), index.sortSubset(records, "de"))
    }

    @Test
    fun `a narrowed subset is ordered the same way as the whole set`() {
        val subset = listOf(records[3], records[0], records[1])

        assertEquals(listOf(2L, 1L, 4L), index.sortSubset(subset, null).map { it.id })
        assertEquals(listOf(1L, 2L, 4L), index.sortSubset(subset, "de").map { it.id })
    }

    private class Record(
        val id: Long,
        private val base: String,
        private val translations: Map<String, String> = emptyMap(),
    ) {
        fun name(languageCode: String?): String =
            languageCode?.let { translations[it] } ?: base
    }
}
