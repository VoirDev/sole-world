package dev.voir.sole.world.api.dataset.index

import java.text.CollationKey
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps one ordering of a record set per translation language.
 *
 * Records are ordered by the name a caller in that language actually sees, not by the raw
 * translation column. Ordering by the raw translation puts untranslated records in a position that
 * does not match the name rendered for them, which makes paging through a localized list skip and
 * repeat records. Resolving first and then ordering keeps a page sequence stable.
 *
 * Orderings are built on first use and cached, so a deployment only pays for the languages its
 * callers actually ask for.
 *
 * @param records Every record in the set, in dataset order.
 * @param displayName Resolves the name shown for a record in a given language.
 * @param tieBreaker Stable identifier used to order records that share a display name.
 */
class LocalizedIndex<T : Any>(
    private val records: List<T>,
    private val displayName: (T, String?) -> String,
    private val tieBreaker: (T) -> Long,
) {
    private val orderings = ConcurrentHashMap<String, List<T>>()

    /** Number of records in the set. */
    val size: Int get() = records.size

    /**
     * Returns every record ordered by its display name in the requested language.
     * @param languageCode Internal language code, or null for base data.
     * @return Records in display order.
     */
    fun sorted(languageCode: String?): List<T> {
        return orderings.computeIfAbsent(languageCode ?: BASE_LANGUAGE) { key ->
            val language = key.takeIf { it != BASE_LANGUAGE }
            sort(records, language)
        }
    }

    /**
     * Orders an arbitrary subset the same way [sorted] orders the whole set.
     *
     * Used for relationship and filtered results, which are too varied to cache. A subset that is in
     * fact the whole set — an unfiltered list request — is answered from the cache instead, which is
     * what keeps a plain `/v1/cities` page from re-sorting 150,000 records on every request.
     *
     * @param subset Records to order.
     * @param languageCode Internal language code, or null for base data.
     * @return The subset in display order.
     */
    fun sortSubset(subset: List<T>, languageCode: String?): List<T> {
        if (subset === records) {
            return sorted(languageCode)
        }

        if (subset.size < 2) {
            return subset
        }

        return sort(subset, languageCode)
    }

    private fun sort(values: List<T>, languageCode: String?): List<T> {
        // Collators are not thread safe, so each sort gets its own.
        val collator = collatorFor(languageCode)

        // Decorate with collation keys: the collator analyses each name once rather than on every
        // one of the n log n comparisons it would otherwise take part in.
        return values
            .map { Sortable(collator.getCollationKey(displayName(it, languageCode)), tieBreaker(it), it) }
            .sortedWith(compareBy({ it.key }, { it.tieBreaker }))
            .map { it.record }
    }

    private class Sortable<T>(
        val key: CollationKey,
        val tieBreaker: Long,
        val record: T,
    )

    private companion object {
        /** Map key standing in for "no translation language", since the map rejects null keys. */
        const val BASE_LANGUAGE = ""

        /**
         * Builds a collator for the requested language.
         * @param languageCode Internal language code, or null for base data.
         * @return Collator ordering names the way a reader of that language expects.
         */
        fun collatorFor(languageCode: String?): Collator {
            val locale = languageCode?.let(Locale::forLanguageTag) ?: Locale.ENGLISH
            return Collator.getInstance(locale)
        }
    }
}
