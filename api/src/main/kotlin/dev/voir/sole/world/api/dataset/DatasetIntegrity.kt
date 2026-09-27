package dev.voir.sole.world.api.dataset

import dev.voir.sole.world.api.dataset.index.IdKey

/**
 * Checks that the bundled dataset refers only to records it actually contains.
 *
 * Parsing proves the shape of each file; nothing proved that the ids in one file exist in another.
 * A dangling reference surfaced at request time as a silently absent `include` or a relationship
 * that returned nothing, which reads like a bug in the API rather than a gap in the data — and a
 * duplicate id was worse, because the store's index quietly kept one record and dropped the other.
 *
 * The dataset is immutable and read once, so checking it here turns a data bug into a startup
 * failure with a precise message, at no cost per request. Every problem is reported together, so
 * correcting the data is one pass rather than one restart per broken row.
 */
object DatasetIntegrity {
    /** Problems listed per rule before the message says how many more there are. */
    private const val EXAMPLES_PER_RULE = 5

    /** Range a currency's popularity score may fall in. */
    private val POPULARITY = 0..100

    private const val SQUARE = "square"
    private const val WIDE = "wide"

    /** Unicode regional indicator symbols A to Z, the pair that spells a flag emoji. */
    private val REGIONAL_INDICATOR = 0x1F1E6..0x1F1FF

    /**
     * Verifies every cross-reference in a parsed dataset.
     * @param dataset Freshly parsed dataset.
     * @throws IllegalStateException When any reference does not resolve, or any id is not unique.
     */
    fun check(dataset: RawDataset) {
        val problems = Problems()

        val mediaAssets = problems.uniqueIds("media assets", dataset.mediaAssets) { it.id }
        val flags = problems.uniqueIds("flags", dataset.flags) { it.id }
        val regions = problems.uniqueIds("regions", dataset.regions) { it.id }
        val subregions = problems.uniqueIds("subregions", dataset.regions.flatMap { it.subregions }) { it.id }
        val currencies = problems.uniqueIds("currencies", dataset.currencies) { it.id }
        val cryptos = problems.uniqueIds("cryptocurrencies", dataset.cryptos) { it.id }
        val languages = problems.uniqueIds("languages", dataset.languages) { it.id }
        val timezones = problems.uniqueIds("timezones", dataset.timezones) { it.id }
        problems.uniqueIds("central banks", dataset.centralBanks) { it.id }
        val countries = problems.uniqueIds("countries", dataset.countries) { it.id }

        val states = dataset.countries.flatMap { it.states }
        problems.uniqueIds("states", states) { it.id }
        problems.uniqueIds("cities", states.flatMap { it.cities }) { it.id }

        val assetsById = dataset.mediaAssets.associateBy { it.id }
        for (flag in dataset.flags) {
            problems.resolves("flag ${flag.id} square", flag.square, mediaAssets, "media asset")
            problems.resolves("flag ${flag.id} wide", flag.wide, mediaAssets, "media asset")

            val square = flag.square?.let(assetsById::get)
            val wide = flag.wide?.let(assetsById::get)

            // A flag naming an image of the wrong shape is serving the wrong picture, and nothing
            // downstream would notice: the id resolves and the response is well formed.
            if (square != null && square.imageAspectRatio != SQUARE) {
                problems.wrongShape("flag ${flag.id} square", square.id, square.imageAspectRatio, SQUARE)
            }
            if (wide != null && wide.imageAspectRatio != WIDE) {
                problems.wrongShape("flag ${flag.id} wide", wide.id, wide.imageAspectRatio, WIDE)
            }

            // Both renditions of one flag are the same picture at two shapes, so they carry the same
            // key. This is what catches a flag that copied a neighbour's image id.
            if (square != null && wide != null && square.key != wide.key) {
                problems.differentPictures(flag.id, square.key, wide.key)
            }

            // Copying a neighbour's flag wholesale keeps both renditions in step, so the check
            // above cannot see it. The emoji says which country the flag is for, and the asset key
            // says which country the picture is of; when they disagree the flag serves the wrong
            // country's colours, which is exactly how Curaçao came to show the Cape Verde flag.
            val territory = territoryOf(flag.emoji)
            if (territory != null && square != null && square.key != territory) {
                problems.wrongTerritory(flag.id, flag.caption, territory, square.key)
            }
        }

        for (currency in dataset.currencies) {
            problems.inRange("currency ${currency.id} popularity", currency.popularity, POPULARITY)
            problems.resolves("currency ${currency.id} flagId", currency.flagId, flags, "flag")
            problems.resolves(
                "currency ${currency.id} replacedBy",
                currency.replacedBy,
                currencies,
                "currency",
            )
        }

        for (crypto in dataset.cryptos) {
            problems.resolves("cryptocurrency ${crypto.id} logoId", crypto.logoId, mediaAssets, "media asset")
        }

        for (language in dataset.languages) {
            problems.resolves("language ${language.id} flagId", language.flagId, flags, "flag")
        }

        for (country in dataset.countries) {
            problems.resolves("country ${country.id} regionId", country.regionId, regions, "region")
            problems.resolves(
                "country ${country.id} subregionId",
                country.subregionId,
                subregions,
                "subregion",
            )
            problems.resolves("country ${country.id} flagId", country.flagId, flags, "flag")
            problems.allResolve(
                "country ${country.id} currencyIds",
                country.currencyIds,
                currencies,
                "currency",
            )
            problems.allResolve(
                "country ${country.id} timezoneIds",
                country.timezoneIds,
                timezones,
                "timezone",
            )
            problems.allResolve(
                "country ${country.id} officialLanguageIds",
                country.officialLanguageIds,
                languages,
                "language",
            )
            problems.allResolve(
                "country ${country.id} otherLanguageIds",
                country.otherLanguageIds,
                languages,
                "language",
            )
        }

        for (bank in dataset.centralBanks) {
            problems.allResolve("central bank ${bank.id} countryIds", bank.countryIds, countries, "country")
            problems.allResolve(
                "central bank ${bank.id} currencyIds",
                bank.currencyIds,
                currencies,
                "currency",
            )
        }

        checkTranslationLanguages(dataset, problems)

        problems.failIfAny()
    }

    /**
     * Verifies that every translation is tagged with a language the dataset knows.
     *
     * A translation's `languageCode` is a language tag rather than a foreign key, so nothing above
     * could catch one that names a language the dataset does not carry. It reads as a missing
     * translation instead of a broken row: the locale simply never matches, and the caller is
     * served base data for a name that was in fact translated. Region-qualified tags such as
     * `pt-BR` are legitimate, and are checked through their primary subtag.
     */
    private fun checkTranslationLanguages(dataset: RawDataset, problems: Problems) {
        val known = dataset.languages.mapTo(HashSet()) { it.code.lowercase() }

        fun check(what: String, codes: List<String>) {
            for (code in codes) {
                if (code.substringBefore('-').lowercase() !in known) {
                    problems.unknownLanguage(what, code)
                }
            }
        }

        for (region in dataset.regions) {
            check("region ${region.id}", region.translations.map { it.languageCode })
            for (subregion in region.subregions) {
                check("subregion ${subregion.id}", subregion.translations.map { it.languageCode })
            }
        }
        for (timezone in dataset.timezones) {
            check("timezone ${timezone.id}", timezone.translations.map { it.languageCode })
        }
        for (currency in dataset.currencies) {
            check("currency ${currency.id}", currency.translations.map { it.languageCode })
        }
        for (language in dataset.languages) {
            check("language ${language.id}", language.translations.map { it.languageCode })
        }
        for (bank in dataset.centralBanks) {
            check("central bank ${bank.id}", bank.translations.map { it.languageCode })
        }
        for (country in dataset.countries) {
            check("country ${country.id}", country.translations.map { it.languageCode })
            for (state in country.states) {
                check("state ${state.id}", state.translations.map { it.languageCode })
                for (city in state.cities) {
                    check("city ${city.id}", city.translations.map { it.languageCode })
                }
            }
        }
    }

    /**
     * Reads the territory a flag emoji stands for, as a lowercase ISO 3166-1 alpha-2 code.
     *
     * A country flag emoji is a pair of regional indicator symbols spelling that code. Subdivision
     * and organisation flags are drawn some other way, and have no code to check against.
     *
     * @param emoji Flag emoji from the record.
     * @return Territory code, or null when the emoji does not name one.
     */
    private fun territoryOf(emoji: String): String? {
        val indicators = emoji.codePoints().toArray().filter { it in REGIONAL_INDICATOR }
        if (indicators.size != 2 || indicators.size != emoji.codePoints().count().toInt()) {
            return null
        }

        return indicators
            .map { 'a' + (it - REGIONAL_INDICATOR.first) }
            .joinToString("")
    }

    /** Collects every problem found so one startup failure can report all of them. */
    private class Problems {
        private val byRule = LinkedHashMap<String, MutableList<String>>()

        /**
         * Records the ids of a collection, reporting any that appear more than once.
         *
         * Identifiers are looked up regardless of case, so two that differ only in case are the
         * same identifier: only one of them could ever be found. An identifier that is blank or
         * padded with whitespace is reported too, because a lookup trims what it is given and
         * would never match it.
         *
         * @return Every id in the collection as spelled, whether or not duplicates were found.
         * References are checked against this spelling, which keeps the data using one.
         */
        fun <T, ID : Any> uniqueIds(collection: String, records: List<T>, id: (T) -> ID): Set<ID> {
            val seen = HashSet<ID>(records.size)
            val keys = HashSet<Any>(records.size)
            for (record in records) {
                val value = id(record)
                if (value is String && (value.isBlank() || value != value.trim())) {
                    add("blank or padded $collection ids", "'$value'")
                }
                if (!keys.add(if (value is String) IdKey.of(value) else value)) {
                    add("duplicate $collection ids, ignoring case", "$value")
                }
                seen += value
            }

            return seen
        }

        /** Records a reference that does not resolve. Absent references are not references. */
        fun <ID> resolves(what: String, reference: ID?, known: Set<ID>, target: String) {
            if (reference != null && reference !in known) {
                add("$target references that do not resolve", "$what=$reference")
            }
        }

        /** Records a score outside the range the published contract promises. */
        fun inRange(what: String, value: Int, allowed: IntRange) {
            if (value !in allowed) {
                add("scores outside their documented range", "$what=$value is not in $allowed")
            }
        }

        /** Records a reference to an image of the wrong aspect ratio. */
        fun wrongShape(what: String, assetId: String, actual: String, expected: String) {
            add("media assets of the wrong shape", "$what=$assetId is $actual, expected $expected")
        }

        /** Records a translation tagged with a language the dataset does not carry. */
        fun unknownLanguage(what: String, code: String) {
            add("translations in a language the dataset does not carry", "$what languageCode='$code'")
        }

        /** Records a flag whose picture belongs to a different territory than its emoji. */
        fun wrongTerritory(flagId: String, caption: String, expected: String, actual: String) {
            add(
                "flags showing another territory's picture",
                "flag $flagId '$caption' expects '$expected' but shows '$actual'",
            )
        }

        /** Records a flag whose two renditions are pictures of different things. */
        fun differentPictures(flagId: String, squareKey: String, wideKey: String) {
            add(
                "flags whose square and wide images are different pictures",
                "flag $flagId square='$squareKey' wide='$wideKey'",
            )
        }

        /** Records every unresolved reference in a list. */
        fun <ID> allResolve(what: String, references: List<ID>, known: Set<ID>, target: String) {
            references.filterNot { it in known }.forEach {
                add("$target references that do not resolve", "$what=$it")
            }
        }

        private fun add(rule: String, detail: String) {
            byRule.getOrPut(rule) { mutableListOf() } += detail
        }

        /** Fails startup when anything was recorded, naming every rule and some of its examples. */
        fun failIfAny() {
            if (byRule.isEmpty()) {
                return
            }

            val report = byRule.entries.joinToString("\n") { (rule, details) ->
                val shown = details.take(EXAMPLES_PER_RULE).joinToString(", ")
                val more = details.size - minOf(details.size, EXAMPLES_PER_RULE)
                "  $rule (${details.size}): $shown" + if (more > 0) ", and $more more" else ""
            }

            error("The bundled dataset is not internally consistent:\n$report")
        }
    }
}
