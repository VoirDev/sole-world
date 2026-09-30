package dev.voir.sole.world.api.dataset.index

import java.text.Normalizer

/**
 * Text normalization and similarity scoring shared by every searchable feature.
 *
 * Both sides of a comparison are folded the same way, which is what makes accented, full-width and
 * Cyrillic queries match: the comparison never depends on the script or the case the caller happened
 * to type. Dataset names are folded once when a store is built and a query once per request, so a
 * search never re-derives either side — see [FoldedText].
 */
object TextIndex {
    /** Shortest query that is scored by trigram similarity; below this, prefixes carry the signal. */
    const val MIN_TRIGRAM_QUERY_LENGTH = 4

    /** Similarity below this is treated as noise rather than a weak match. */
    const val MIN_TRIGRAM_SIMILARITY = 0.3

    private val COMBINING_MARKS = "\\p{Mn}+".toRegex()

    /** Dots and apostrophes, which sit inside a word: "U.S.A.", "d'Ivoire", "Мʼянма". */
    private val WORD_INTERNAL_PUNCTUATION = "[.'‘’ʼ`]".toRegex()

    /** Every other run of punctuation or whitespace, which separates words: "Guinea-Bissau". */
    private val WORD_SEPARATORS = "[\\p{P}\\s]+".toRegex()

    /**
     * Folds text into the form used for every comparison.
     *
     * Compatibility decomposition splits accented characters into a base letter plus a combining
     * mark, the marks are dropped, and the result is lowercased. Punctuation is then reduced to word
     * boundaries, so the ways people write the same name compare equal: dots and apostrophes vanish,
     * any other punctuation becomes a single space, and a run of single letters closes up. "U.S.A.",
     * "U. S. A." and "USA" all fold to `usa`, and "Guinea-Bissau" to `guinea bissau`.
     *
     * Text that is nothing but punctuation keeps it, so that a query such as `-` still means what
     * it says rather than folding away into no query at all.
     *
     * @param value Raw text from the dataset or from a caller.
     * @return Folded text safe to compare against other folded text.
     */
    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val folded = COMBINING_MARKS.replace(decomposed, "").lowercase().trim()

        val words = WORD_SEPARATORS.replace(WORD_INTERNAL_PUNCTUATION.replace(folded, ""), " ").trim()
        if (words.isEmpty()) {
            return folded
        }

        return closeUpInitials(words)
    }

    /** Joins consecutive single-letter words, which is how initials were spelled out: `u s a`. */
    private fun closeUpInitials(words: String): String {
        if (' ' !in words) {
            return words
        }

        val result = StringBuilder(words.length)
        var previousWasInitial = false
        for (word in words.split(' ')) {
            val initial = word.length == 1 && word[0].isLetter()
            if (result.isNotEmpty() && !(initial && previousWasInitial)) {
                result.append(' ')
            }
            result.append(word)
            previousWasInitial = initial
        }

        return result.toString()
    }

    /**
     * Splits folded text into the trigrams used for similarity scoring.
     *
     * The value is padded so that the first and last characters carry the same weight as the middle
     * ones, which is what makes a wrong first letter cost less than a wrong middle.
     *
     * @param normalized Already-folded text.
     * @return Distinct trigrams for the value, or an empty set when it is too short to score.
     */
    fun trigrams(normalized: String): Set<String> {
        if (normalized.isEmpty()) {
            return emptySet()
        }

        val padded = "  $normalized "
        return (0..padded.length - 3).mapTo(mutableSetOf()) { padded.substring(it, it + 3) }
    }

    /**
     * Scores how similar two already-folded strings are.
     *
     * This is the Jaccard index over their trigram sets: shared trigrams divided by total distinct
     * trigrams. Identical strings score 1.0 and strings with nothing in common score 0.0.
     *
     * @param queryTrigrams Trigrams of the query, computed once per search.
     * @param candidate Folded candidate text.
     * @return Similarity in 0.0..1.0.
     */
    fun similarity(queryTrigrams: Set<String>, candidate: String): Double {
        if (queryTrigrams.isEmpty()) {
            return 0.0
        }

        val candidateTrigrams = trigrams(candidate)
        if (candidateTrigrams.isEmpty()) {
            return 0.0
        }

        val shared = queryTrigrams.count { it in candidateTrigrams }
        if (shared == 0) {
            return 0.0
        }

        val distinct = queryTrigrams.size + candidateTrigrams.size - shared
        return shared.toDouble() / distinct.toDouble()
    }
}

/**
 * Text that has already been through [TextIndex.normalize].
 *
 * Folding is the expensive half of a comparison — decomposition, a regex over combining marks and a
 * lowercase pass — so dataset text is folded when its store is built and kept in this form. The
 * wrapper makes "already folded" a property of the type rather than of a convention, so raw and
 * folded text cannot be swapped at a call site; it is a value class, so it costs nothing at runtime.
 *
 * @property value The folded text.
 */
@JvmInline
value class FoldedText private constructor(val value: String) {
    /** True when there is nothing left to compare against after folding. */
    val isEmpty: Boolean get() = value.isEmpty()

    companion object {
        /** Stands in for text that folds away to nothing, and for absent values. */
        val EMPTY = FoldedText("")

        /**
         * Folds raw text for later comparison.
         * @param raw Raw text, or null for an absent value.
         * @return Folded text, or [EMPTY] when there was nothing to fold.
         */
        fun of(raw: String?): FoldedText =
            if (raw == null) EMPTY else FoldedText(TextIndex.normalize(raw))
    }
}

/**
 * A folded query, prepared once and reused across every candidate in one search.
 * @property raw Trimmed query exactly as the caller supplied it.
 * @property normalized Folded query used for all comparisons.
 * @property trigrams Trigrams of [normalized], or empty when the query is too short to score.
 */
class SearchQuery private constructor(
    val raw: String,
    val normalized: String,
    val trigrams: Set<String>,
) {
    /**
     * Scores a folded candidate against this query using the supplied match weights.
     *
     * Exact and prefix matches always outrank substring matches, and trigram similarity is only
     * consulted when nothing stronger matched, so a fuzzy hit can never displace a literal one.
     *
     * @param candidate Folded candidate text; empty candidates never match.
     * @param weight Score awarded for an exact match on this field. Weaker match kinds scale down.
     * @return Score for the candidate, or 0 when it does not match at all.
     */
    fun score(candidate: FoldedText, weight: Int): Int {
        val value = candidate.value
        if (value.isEmpty()) {
            return 0
        }

        return when {
            value == normalized -> weight
            value.startsWith(normalized) -> weight - PREFIX_PENALTY
            value.contains(normalized) -> weight - SUBSTRING_PENALTY
            else -> fuzzyScore(value, weight)
        }
    }

    /**
     * Scores a raw candidate against this query, folding it first.
     *
     * For the short scalar fields — ISO codes, calling codes, symbols — where the candidate set is a
     * few hundred records and folding is cheaper than carrying a second copy of each field. Name
     * collections, which are three orders of magnitude larger, are folded once by [LocalizedName].
     *
     * @param candidate Raw candidate text; null and blank candidates never match.
     * @param weight Score awarded for an exact match on this field.
     * @return Score for the candidate, or 0 when it does not match at all.
     */
    fun score(candidate: String?, weight: Int): Int = score(FoldedText.of(candidate), weight)

    /**
     * Tests whether a folded candidate literally contains this query.
     *
     * Used for the `query` filter arguments, which narrow a known set rather than rank it. Filters
     * stay literal on purpose: a fuzzy filter would change the reported total item count, so a
     * caller paging through "cities matching South" could not trust the count they were given.
     *
     * @param candidate Folded candidate text; empty candidates never match.
     * @return True when the candidate contains the folded query.
     */
    fun matches(candidate: FoldedText): Boolean =
        candidate.value.isNotEmpty() && candidate.value.contains(normalized)

    private fun fuzzyScore(normalizedCandidate: String, weight: Int): Int {
        if (trigrams.isEmpty()) {
            return 0
        }

        val similarity = TextIndex.similarity(trigrams, normalizedCandidate)
        if (similarity < TextIndex.MIN_TRIGRAM_SIMILARITY) {
            return 0
        }

        // Fuzzy matches occupy the band below every literal match of the same field.
        val fuzzyWeight = weight - FUZZY_PENALTY
        return if (fuzzyWeight <= 0) 0 else (fuzzyWeight * similarity).toInt().coerceAtLeast(1)
    }

    companion object {
        private const val PREFIX_PENALTY = 5
        private const val SUBSTRING_PENALTY = 10
        private const val FUZZY_PENALTY = 20

        /**
         * Prepares a caller-supplied query for scoring.
         * @param raw Raw query text.
         * @return Prepared query, or null when the query is blank after trimming.
         */
        fun of(raw: String): SearchQuery? {
            val trimmed = raw.trim()
            val normalized = TextIndex.normalize(trimmed)
            if (normalized.isEmpty()) {
                return null
            }

            val trigrams = if (normalized.length >= TextIndex.MIN_TRIGRAM_QUERY_LENGTH) {
                TextIndex.trigrams(normalized)
            } else {
                emptySet()
            }

            return SearchQuery(raw = trimmed, normalized = normalized, trigrams = trigrams)
        }
    }
}
