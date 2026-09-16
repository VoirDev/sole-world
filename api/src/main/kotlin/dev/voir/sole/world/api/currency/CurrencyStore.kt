package dev.voir.sole.world.api.currency

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.dataset.index.SortOrder
import dev.voir.sole.world.api.model.CurrencyData
import kotlinx.datetime.LocalDate
import org.springframework.stereotype.Component

/** Read model for currencies and their country and central bank relationships. */
@Component
class CurrencyStore(dataset: RawDataset) {
    private val records: List<CurrencyRecord> = dataset.currencies.map { json ->
        CurrencyRecord(
            id = json.id,
            iso3 = json.iso3,
            isoNumeric = json.isoNumeric,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.languageCode to it.name },
            ),
            decimalDigits = json.decimalDigits,
            popularity = json.popularity,
            description = json.description,
            descriptionTranslations = json.translations
                .mapNotNull { translation -> translation.description?.let { translation.languageCode to it } }
                .toMap(),
            nativeName = json.nativeName,
            symbol = json.symbol,
            year = json.year,
            introducedDate = parseDate(json.introducedDate, json.iso3, "introducedDate"),
            obsolete = json.obsolete,
            obsoleteAt = parseDate(json.obsoleteAt, json.iso3, "obsoleteAt"),
            replacedById = json.replacedBy,
            flagId = json.flagId,
        )
    }

    private val byId: Map<Long, CurrencyRecord> = records.associateBy { it.id }

    private val byIso3: Map<String, CurrencyRecord> = records.associateBy { it.iso3.lowercase() }

    private val byIsoNumeric: Map<String, CurrencyRecord> = records.associateBy { it.isoNumeric.lowercase() }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    private val idsByCountryId: Map<Long, List<Long>> = dataset.countries
        .associate { country -> country.id to country.currencyIds }

    private val idsByCentralBankId: Map<Long, List<Long>> = dataset.centralBanks
        .associate { bank -> bank.id to bank.currencyIds }

    /** Total number of currencies. */
    val size: Int get() = records.size

    /**
     * Loads one currency.
     * @param id Currency identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching currency, or null when none exists.
     */
    fun byId(id: Long, languageCode: String?): CurrencyData? = byId[id]?.localized(languageCode)

    /**
     * Loads several currencies, skipping identifiers that do not exist.
     * @param ids Currency identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching currencies in request order.
     */
    fun byIds(ids: List<Long>, languageCode: String?): List<CurrencyData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Resolves a currency from an ISO alpha code, ISO numeric code, or numeric identifier.
     * @param identifier Caller-supplied identifier.
     * @param withObsolete Whether an obsolete currency may be returned; defaults to false.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching currency, or null when nothing matches the obsolescence constraint.
     */
    fun resolve(identifier: String, withObsolete: Boolean?, languageCode: String?): CurrencyData? {
        val key = identifier.trim().lowercase()
        val record = byIso3[key]
            ?: byIsoNumeric[key]
            ?: key.toLongOrNull()?.let(byId::get)
            ?: return null

        if (record.obsolete && withObsolete != true) {
            return null
        }

        return record.localized(languageCode)
    }

    /**
     * Returns one page of currencies.
     *
     * Without a `sort` the ordering is whichever is more useful: relevance when the caller searched,
     * and popularity otherwise. An explicit `sort` wins over both, including over relevance, because
     * a caller who names an ordering means it.
     *
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @param obsolete Restricts results to obsolete or active currencies; null returns both.
     * @param sort Field to order by, or null for the default ordering.
     * @param order Direction, or null for the sort field's own default.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        obsolete: Boolean? = null,
        sort: CurrencySort? = null,
        order: SortOrder? = null,
    ): Page<CurrencyData> {
        val candidates = if (obsolete == null) records else records.filter { it.obsolete == obsolete }

        // Searching both selects and orders. A sort replaces the ordering it produced, never the
        // selection: `?query=dollar&sort=name` is still only the currencies called dollar.
        // Equally relevant matches are broken by popularity, so searching "dollar" opens on the one
        // the caller almost certainly meant.
        val matching = if (query == null) {
            candidates
        } else {
            Ranking.rank(candidates, query, CurrencyRecord::scoreOf) { -it.popularity.toLong() }
        }

        val ordered = when {
            sort != null -> sorted(matching, sort, order ?: sort.defaultOrder, languageCode)
            query != null -> matching
            else -> sorted(matching, CurrencySort.POPULARITY, SortOrder.DESC, languageCode)
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the currencies used by a country.
     * @param countryId Country identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Currencies ordered by popularity, most widely used first.
     */
    fun byCountryId(countryId: Long, languageCode: String?): List<CurrencyData> =
        localizedSubset(idsByCountryId[countryId], languageCode)

    /**
     * Loads the currencies issued by a central bank.
     * @param centralBankId Central bank identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Currencies ordered by popularity, most widely used first.
     */
    fun byCentralBankId(centralBankId: Long, languageCode: String?): List<CurrencyData> =
        localizedSubset(idsByCentralBankId[centralBankId], languageCode)

    private fun localizedSubset(ids: List<Long>?, languageCode: String?): List<CurrencyData> {
        if (ids.isNullOrEmpty()) {
            return emptyList()
        }

        val matching = ids.mapNotNull(byId::get)
        return sorted(matching, CurrencySort.POPULARITY, SortOrder.DESC, languageCode)
            .map { it.localized(languageCode) }
    }

    /**
     * Orders currencies by one field.
     *
     * Every ordering starts from the localized name ordering and re-sorts it, because Kotlin's sort
     * is stable: records that tie on the requested field come out alphabetically rather than in
     * whatever order the dataset happens to hold them, which is what keeps paging through a tie
     * — the 30-odd currencies that share a popularity score — from skipping and repeating records.
     */
    private fun sorted(
        candidates: List<CurrencyRecord>,
        sort: CurrencySort,
        order: SortOrder,
        languageCode: String?,
    ): List<CurrencyRecord> {
        val byName = index.sortSubset(candidates, languageCode)
        val descending = order == SortOrder.DESC

        return when (sort) {
            CurrencySort.NAME ->
                if (descending) byName.reversed() else byName

            CurrencySort.POPULARITY -> if (descending) {
                byName.sortedByDescending { it.popularity }
            } else {
                byName.sortedBy { it.popularity }
            }

            CurrencySort.CODE ->
                if (descending) byName.sortedByDescending { it.iso3 } else byName.sortedBy { it.iso3 }

            // A currency whose introduction year is unknown sorts last in either direction: it is
            // absent from the timeline rather than at one end of it.
            CurrencySort.YEAR -> {
                val years = if (descending) reverseOrder<Int>() else naturalOrder<Int>()
                byName.sortedWith(compareBy(nullsLast(years)) { it.year })
            }
        }
    }

    private companion object {
        /**
         * Parses an optional ISO-8601 date from the dataset.
         *
         * A malformed date fails startup rather than quietly becoming null, because a null here is
         * indistinguishable from "not known" in the published API.
         */
        fun parseDate(value: String?, iso3: String, field: String): LocalDate? {
            val text = value?.trim()?.ifEmpty { null } ?: return null

            return try {
                LocalDate.parse(text)
            } catch (failure: IllegalArgumentException) {
                throw IllegalStateException("Currency $iso3 has an invalid $field: '$text'", failure)
            }
        }
    }
}

/**
 * A currency as held in memory, keeping every translation so names resolve per request.
 * @property id Stable currency identifier.
 * @property iso3 ISO 4217 alpha code.
 * @property isoNumeric ISO 4217 numeric code.
 * @property name Display name in every available language.
 * @property decimalDigits Minor-unit digits commonly used.
 * @property popularity How widely the currency is used today, from 0 to 100.
 * @property description Base description, when available.
 * @property descriptionTranslations Translated descriptions by internal language code.
 * @property nativeName Name in the currency's own language, when available.
 * @property symbol Currency symbol, when available.
 * @property year Introduction year, when known.
 * @property introducedDate Introduction date, when known.
 * @property obsolete Whether the currency is obsolete.
 * @property obsoleteAt Obsolescence date, when known.
 * @property replacedById Replacement currency identifier, when available.
 * @property flagId Shared flag identifier, when available.
 */
private class CurrencyRecord(
    val id: Long,
    val iso3: String,
    val isoNumeric: String,
    val name: LocalizedName,
    val decimalDigits: Int,
    val popularity: Int,
    val description: String?,
    val descriptionTranslations: Map<String, String>,
    val nativeName: String?,
    val symbol: String?,
    val year: Int?,
    val introducedDate: LocalDate?,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val replacedById: Long?,
    val flagId: Long?,
) {
    /** Scores this currency against a search query, strongest field first. */
    fun scoreOf(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)

        return maxOf(
            nameScore,
            query.score(iso3, ISO3_WEIGHT),
            query.score(isoNumeric, ISO_NUMERIC_WEIGHT),
            query.score(nativeName, NATIVE_NAME_WEIGHT),
            query.score(symbol, SYMBOL_WEIGHT),
        )
    }

    fun localized(languageCode: String?) = CurrencyData(
        id = id,
        iso3 = iso3,
        isoNumeric = isoNumeric,
        name = name.resolve(languageCode),
        decimalDigits = decimalDigits,
        popularity = popularity,
        description = languageCode?.let { descriptionTranslations[it] } ?: description,
        nativeName = nativeName,
        symbol = symbol,
        year = year,
        introducedDate = introducedDate,
        obsolete = obsolete,
        obsoleteAt = obsoleteAt,
        replacedById = replacedById,
        flagId = flagId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val ISO3_WEIGHT = 80
        const val ISO_NUMERIC_WEIGHT = 70
        const val NATIVE_NAME_WEIGHT = 60
        const val SYMBOL_WEIGHT = 40
    }
}
