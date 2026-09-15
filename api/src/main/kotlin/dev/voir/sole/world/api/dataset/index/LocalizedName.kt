package dev.voir.sole.world.api.dataset.index

/**
 * Localized names for one record, keyed by the internal language code.
 *
 * Records always carry a base (English) name, so resolution never fails — a missing translation
 * falls back to the base name. Ordering is done on the *resolved* name rather than on the raw
 * translation, so a page of results is sorted by what the caller actually sees.
 *
 * Every distinct name is also folded for search when the record is built. Cities alone contribute
 * around 155,000 names, so folding them per request was the dominant cost of a search; doing it here
 * means a query folds itself once and then only compares.
 *
 * @property base Base English name always present on the record.
 * @property translations Translated names by internal language code.
 * @property searchNames Every distinct name this record is known by, folded for comparison.
 */
class LocalizedName private constructor(
    val base: String,
    private val translations: Map<String, String>,
    private val searchNames: List<FoldedText>,
) {
    /**
     * Resolves the name a caller should see.
     * @param languageCode Internal language code, or null for base data.
     * @return Translated name when one exists, otherwise the base name.
     */
    fun resolve(languageCode: String?): String {
        if (languageCode == null) {
            return base
        }

        return translations[languageCode] ?: base
    }

    /**
     * Scores this record's best-matching name against a search query.
     * @param query Prepared search query.
     * @param weight Score awarded for an exact name match.
     * @return Highest score any of the record's names earns.
     */
    fun score(query: SearchQuery, weight: Int): Int = searchNames.maxOf { query.score(it, weight) }

    /**
     * Tests this record against a literal name filter, in any language it is known by.
     * @param query Prepared search query.
     * @return True when any of the record's names contains the query.
     */
    fun matches(query: SearchQuery): Boolean = searchNames.any(query::matches)

    companion object {
        /**
         * Builds localized names from a record's translation list.
         * @param base Base English name.
         * @param translations Language code to translated name pairs; later duplicates are ignored.
         * @return Localized names for the record.
         */
        fun of(base: String, translations: List<Pair<String, String>>): LocalizedName {
            if (translations.isEmpty()) {
                return LocalizedName(base, emptyMap(), listOf(FoldedText.of(base)))
            }

            val byCode = HashMap<String, String>(translations.size)
            for ((code, name) in translations) {
                byCode.putIfAbsent(code, name)
            }

            // Distinct on the folded form: two translations that differ only by case or accent fold
            // to the same text and would otherwise be compared twice for every query.
            val searchNames = buildList {
                add(base)
                addAll(byCode.values)
            }.mapTo(LinkedHashSet()) { FoldedText.of(it) }.toList()

            return LocalizedName(base, byCode, searchNames)
        }
    }
}
