package dev.voir.sole.world.api.country

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.CountryData
import org.springframework.stereotype.Component

/** Read model for countries and every relationship that resolves back to a country. */
@Component
class CountryStore(dataset: RawDataset) {
    private val records: List<CountryRecord> = dataset.countries.map { json ->
        CountryRecord(
            id = json.id,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.locale to it.name },
            ),
            nativeName = json.nativeName,
            iso3 = json.iso3,
            iso2 = json.iso2,
            isoNumeric = json.numericCode,
            phoneCode = json.phoneCode,
            tld = json.tld,
            latitude = json.latitude,
            longitude = json.longitude,
            flagId = json.flagId,
            regionId = json.regionId,
            subregionId = json.subregionId,
        )
    }

    private val byId: IdIndex<CountryRecord> = IdIndex.of(records) { it.id }

    private val byIso3: IdIndex<CountryRecord> = IdIndex.of(records) { it.iso3 }

    private val byIsoNumeric: IdIndex<CountryRecord> = IdIndex.of(records) { it.isoNumeric }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    private val byRegionId: IdIndex<List<CountryRecord>> = IdIndex.grouped(records) { it.regionId }

    private val bySubregionId: IdIndex<List<CountryRecord>> = IdIndex.grouped(records) { it.subregionId }

    private val idsByCurrencyId: IdIndex<List<String>> = invert(dataset) { it.currencyIds }

    private val idsByLanguageId: IdIndex<List<String>> =
        invert(dataset) { (it.officialLanguageIds + it.otherLanguageIds).distinct() }

    private val idsByTimezoneId: IdIndex<List<String>> = invert(dataset) { it.timezoneIds }

    private val idsByCentralBankId: IdIndex<List<String>> =
        IdIndex.from(dataset.centralBanks.associate { bank -> bank.id to bank.countryIds })

    /** Total number of countries. */
    val size: Int get() = records.size

    /**
     * Loads one country.
     * @param id Country identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching country, or null when none exists.
     */
    fun byId(id: String, languageCode: String?): CountryData? = byId[id]?.localized(languageCode)

    /**
     * Loads one country by its identifier or by one of its other ISO 3166-1 codes.
     * @param identifier Alpha-2 identifier, alpha-3 code, or numeric code, in any case.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching country, or null when none exists.
     */
    fun byIdentifier(identifier: String, languageCode: String?): CountryData? {
        val record = byId[identifier]
            ?: byIso3[identifier]
            ?: byIsoNumeric[identifier]
            ?: return null

        return record.localized(languageCode)
    }

    /**
     * Loads several countries, skipping identifiers that do not exist.
     * @param ids Country identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching countries in request order.
     */
    fun byIds(ids: List<String>, languageCode: String?): List<CountryData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of countries, optionally filtered and searched.
     *
     * Without a query the page is ordered by localized name; with one it is ordered by relevance.
     * Filters are applied before the query, so the reported total always describes the records a
     * caller could page through with the same parameters.
     *
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @param regionId Restrict to one region.
     * @param subregionId Restrict to one subregion.
     * @param currencyId Restrict to countries using one currency.
     * @param languageId Restrict to countries using one language.
     * @param timezoneId Restrict to countries in one timezone.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        regionId: String? = null,
        subregionId: String? = null,
        currencyId: String? = null,
        languageId: String? = null,
        timezoneId: String? = null,
    ): Page<CountryData> {
        val filters = listOfNotNull(
            regionId?.let { byRegionId[it].orEmpty().mapTo(mutableSetOf()) { country -> country.id } },
            subregionId?.let { bySubregionId[it].orEmpty().mapTo(mutableSetOf()) { country -> country.id } },
            currencyId?.let { idsByCurrencyId[it].orEmpty().toSet() },
            languageId?.let { idsByLanguageId[it].orEmpty().toSet() },
            timezoneId?.let { idsByTimezoneId[it].orEmpty().toSet() },
        )

        val candidates = if (filters.isEmpty()) {
            records
        } else {
            records.filter { record -> filters.all { record.id in it } }
        }

        val ordered = if (query == null) {
            index.sortSubset(candidates, languageCode)
        } else {
            Ranking.rank(candidates, query, CountryRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the countries inside a region.
     * @param regionId Region identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Countries ordered by localized name.
     */
    fun byRegionId(regionId: String, languageCode: String?): List<CountryData> =
        localizedSubset(byRegionId[regionId], languageCode)

    /**
     * Loads the countries inside a subregion.
     * @param subregionId Subregion identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Countries ordered by localized name.
     */
    fun bySubregionId(subregionId: String, languageCode: String?): List<CountryData> =
        localizedSubset(bySubregionId[subregionId], languageCode)

    /**
     * Loads the countries that use a currency.
     * @param currencyId Currency identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Countries ordered by localized name.
     */
    fun byCurrencyId(currencyId: String, languageCode: String?): List<CountryData> =
        localizedSubset(idsByCurrencyId[currencyId]?.mapNotNull(byId::get), languageCode)

    /**
     * Loads the countries associated with a language.
     * @param languageId Language identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Countries ordered by localized name.
     */
    fun byLanguageId(languageId: String, languageCode: String?): List<CountryData> =
        localizedSubset(idsByLanguageId[languageId]?.mapNotNull(byId::get), languageCode)

    /**
     * Loads the countries served by a central bank.
     * @param centralBankId Central bank identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Countries ordered by localized name.
     */
    fun byCentralBankId(centralBankId: String, languageCode: String?): List<CountryData> =
        localizedSubset(idsByCentralBankId[centralBankId]?.mapNotNull(byId::get), languageCode)

    private fun localizedSubset(matching: List<CountryRecord>?, languageCode: String?): List<CountryData> {
        if (matching.isNullOrEmpty()) {
            return emptyList()
        }

        return index.sortSubset(matching, languageCode).map { it.localized(languageCode) }
    }

    private companion object {
        /** Inverts a country's outgoing relationship list into a lookup from the related entity. */
        inline fun invert(
            dataset: RawDataset,
            relatedIds: (dev.voir.sole.world.api.dataset.json.CountryJSON) -> List<String>,
        ): IdIndex<List<String>> {
            val index = mutableMapOf<String, MutableList<String>>()
            for (country in dataset.countries) {
                for (relatedId in relatedIds(country)) {
                    index.getOrPut(relatedId) { mutableListOf() }.add(country.id)
                }
            }

            return IdIndex.from(index)
        }
    }
}

/**
 * A country as held in memory, keeping every translation so names resolve per request.
 * @property id Stable country identifier.
 * @property name Display name in every available language.
 * @property nativeName Name in the country's own language, when available.
 * @property iso3 ISO 3166-1 alpha-3 code.
 * @property iso2 ISO 3166-1 alpha-2 code.
 * @property isoNumeric ISO 3166-1 numeric code, kept as text to preserve leading zeros.
 * @property phoneCode International calling code.
 * @property tld Top-level domain, when available.
 * @property latitude Latitude in decimal degrees.
 * @property longitude Longitude in decimal degrees.
 * @property flagId Shared flag identifier, when available.
 * @property regionId Parent region identifier.
 * @property subregionId Parent subregion identifier.
 */
private class CountryRecord(
    val id: String,
    val name: LocalizedName,
    val nativeName: String?,
    val iso3: String,
    val iso2: String,
    val isoNumeric: String,
    val phoneCode: String,
    val tld: String?,
    val latitude: Double,
    val longitude: Double,
    val flagId: String?,
    val regionId: String,
    val subregionId: String,
) {
    /**
     * Scores this country against a search query.
     *
     * The weights reproduce the relevance ladder the SQL implementation used: an exact name beats a
     * prefix, a prefix beats a substring, and identifier fields rank below names throughout.
     */
    fun score(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)

        return maxOf(
            nameScore,
            query.score(nativeName, NATIVE_NAME_WEIGHT),
            query.score(iso2, ISO2_WEIGHT),
            query.score(iso3, ISO3_WEIGHT),
            query.score(isoNumeric, ISO_NUMERIC_WEIGHT),
            query.score(phoneCode, PHONE_CODE_WEIGHT),
            query.score(tld, TLD_WEIGHT),
        )
    }

    fun localized(languageCode: String?) = CountryData(
        id = id,
        name = name.resolve(languageCode),
        nativeName = nativeName,
        iso3 = iso3,
        iso2 = iso2,
        isoNumeric = isoNumeric,
        phoneCode = phoneCode,
        tld = tld,
        latitude = latitude,
        longitude = longitude,
        flagId = flagId,
        regionId = regionId,
        subregionId = subregionId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val ISO2_WEIGHT = 75
        const val ISO3_WEIGHT = 70
        const val ISO_NUMERIC_WEIGHT = 65
        const val NATIVE_NAME_WEIGHT = 50
        const val PHONE_CODE_WEIGHT = 40
        const val TLD_WEIGHT = 30
    }
}
