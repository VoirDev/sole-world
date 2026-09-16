package dev.voir.sole.world.api.dataset

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

    private const val SQUARE = "square"
    private const val WIDE = "wide"

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
        }

        for (currency in dataset.currencies) {
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

        problems.failIfAny()
    }

    /** Collects every problem found so one startup failure can report all of them. */
    private class Problems {
        private val byRule = LinkedHashMap<String, MutableList<String>>()

        /**
         * Records the ids of a collection, reporting any that appear more than once.
         * @return Every id in the collection, whether or not duplicates were found.
         */
        fun <T, ID> uniqueIds(collection: String, records: List<T>, id: (T) -> ID): Set<ID> {
            val seen = HashSet<ID>(records.size)
            for (record in records) {
                if (!seen.add(id(record))) {
                    add("duplicate $collection ids", "${id(record)}")
                }
            }

            return seen
        }

        /** Records a reference that does not resolve. Absent references are not references. */
        fun <ID> resolves(what: String, reference: ID?, known: Set<ID>, target: String) {
            if (reference != null && reference !in known) {
                add("$target references that do not resolve", "$what=$reference")
            }
        }

        /** Records a reference to an image of the wrong aspect ratio. */
        fun wrongShape(what: String, assetId: Long, actual: String, expected: String) {
            add("media assets of the wrong shape", "$what=$assetId is $actual, expected $expected")
        }

        /** Records a flag whose two renditions are pictures of different things. */
        fun differentPictures(flagId: Long, squareKey: String, wideKey: String) {
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
