package dev.voir.sole.world.api.dataset.index

/**
 * Localized names for one record, keyed by the internal language code.
 *
 * Records always carry a base (English) name, so resolution never fails — a missing translation
 * falls back to the base name. Ordering is done on the *resolved* name rather than on the raw
 * translation, so a page of results is sorted by what the caller actually sees.
 *
 * A record may also carry aliases: the other names people use for it, such as "USA" or "Czech
 * Republic". They are grouped by language like the names, so a caller reading German is shown German
 * aliases, but search considers every one of them whatever the caller reads.
 *
 * Every distinct name is also folded for search when the record is built. Cities alone contribute
 * around 155,000 names, so folding them per request was the dominant cost of a search; doing it here
 * means a query folds itself once and then only compares.
 *
 * @property base Base English name always present on the record.
 * @property translations Translated names by internal language code.
 * @property baseAliases English aliases.
 * @property translatedAliases Aliases by internal language code.
 * @property searchNames Every distinct name this record is known by, folded for comparison.
 * @property searchAliases Every distinct alias not already among [searchNames], folded for comparison.
 */
class LocalizedName private constructor(
    val base: String,
    private val translations: Map<String, String>,
    private val baseAliases: List<String>,
    private val translatedAliases: Map<String, List<String>>,
    private val searchNames: List<FoldedText>,
    private val searchAliases: List<FoldedText>,
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
     * Resolves the aliases a caller should see, in the same language as [resolve].
     *
     * Aliases follow the name rather than falling back on their own: when the name falls back to
     * English, so do the aliases, and a translated name without aliases has none.
     *
     * @param languageCode Internal language code, or null for base data.
     * @return Aliases in the language of the resolved name, possibly empty.
     */
    fun aliases(languageCode: String?): List<String> {
        if (languageCode == null || languageCode !in translations) {
            return baseAliases
        }

        return translatedAliases[languageCode].orEmpty()
    }

    /**
     * Scores this record's best-matching name against a search query.
     * @param query Prepared search query.
     * @param weight Score awarded for an exact name match.
     * @return Highest score any of the record's names earns.
     */
    fun score(query: SearchQuery, weight: Int): Int = searchNames.maxOf { query.score(it, weight) }

    /**
     * Scores this record's best-matching alias against a search query.
     * @param query Prepared search query.
     * @param weight Score awarded for an exact alias match.
     * @return Highest score any of the record's aliases earns, or 0 when it has none.
     */
    fun scoreAliases(query: SearchQuery, weight: Int): Int =
        searchAliases.maxOfOrNull { query.score(it, weight) } ?: 0

    /**
     * Tests this record against a literal name filter, in any language it is known by.
     * @param query Prepared search query.
     * @return True when any of the record's names or aliases contains the query.
     */
    fun matches(query: SearchQuery): Boolean =
        searchNames.any(query::matches) || searchAliases.any(query::matches)

    companion object {
        /**
         * Builds localized names from a record's translation list.
         * @param base Base English name.
         * @param translations Language code to translated name pairs; later duplicates are ignored.
         * @param baseAliases English aliases.
         * @param translatedAliases Language code to alias list pairs; later duplicates are ignored.
         * @return Localized names for the record.
         */
        fun of(
            base: String,
            translations: List<Pair<String, String>>,
            baseAliases: List<String> = emptyList(),
            translatedAliases: List<Pair<String, List<String>>> = emptyList(),
        ): LocalizedName {
            if (translations.isEmpty() && baseAliases.isEmpty() && translatedAliases.isEmpty()) {
                return LocalizedName(
                    base = base,
                    translations = emptyMap(),
                    baseAliases = emptyList(),
                    translatedAliases = emptyMap(),
                    searchNames = listOf(FoldedText.of(base)),
                    searchAliases = emptyList(),
                )
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
            }.mapTo(LinkedHashSet()) { FoldedText.of(it) }

            val aliasesByCode = HashMap<String, List<String>>(translatedAliases.size)
            for ((code, aliases) in translatedAliases) {
                if (aliases.isNotEmpty()) {
                    aliasesByCode.putIfAbsent(code, aliases)
                }
            }

            // An alias that folds to one of the names already scores as a name, the stronger match.
            val searchAliases = buildList {
                addAll(baseAliases)
                aliasesByCode.values.forEach(::addAll)
            }.mapTo(LinkedHashSet()) { FoldedText.of(it) }
                .filterNot { it.isEmpty || it in searchNames }

            return LocalizedName(
                base = base,
                translations = byCode,
                baseAliases = baseAliases,
                translatedAliases = aliasesByCode,
                searchNames = searchNames.toList(),
                searchAliases = searchAliases,
            )
        }
    }
}
