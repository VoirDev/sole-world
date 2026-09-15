package dev.voir.sole.world.api.centralbank

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.CentralBankData
import org.springframework.stereotype.Component

/** Read model for central banks and the countries and currencies they serve. */
@Component
class CentralBankStore(dataset: RawDataset) {
    private val records: List<CentralBankRecord> = dataset.centralBanks.map { json ->
        CentralBankRecord(
            id = json.id,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.languageCode to it.name },
            ),
            nativeName = json.nativeName,
            websiteURL = json.websiteURL,
            establishmentYear = json.establishmentYear,
        )
    }

    private val byId: Map<Long, CentralBankRecord> = records.associateBy { it.id }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    private val idsByCountryId: Map<Long, List<Long>> = reverseIndex(dataset) { it.countryIds }

    private val idsByCurrencyId: Map<Long, List<Long>> = reverseIndex(dataset) { it.currencyIds }

    /** Total number of central banks. */
    val size: Int get() = records.size

    /**
     * Loads one central bank.
     * @param id Central bank identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching central bank, or null when none exists.
     */
    fun byId(id: Long, languageCode: String?): CentralBankData? = byId[id]?.localized(languageCode)

    /**
     * Loads several central banks, skipping identifiers that do not exist.
     * @param ids Central bank identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching central banks in request order.
     */
    fun byIds(ids: List<Long>, languageCode: String?): List<CentralBankData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of central banks, ordered by localized name or by relevance when searching.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<CentralBankData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, CentralBankRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the central banks serving a country.
     * @param countryId Country identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Central banks ordered by localized name.
     */
    fun byCountryId(countryId: Long, languageCode: String?): List<CentralBankData> =
        localizedSubset(idsByCountryId[countryId], languageCode)

    /**
     * Loads the central banks that issue a currency.
     * @param currencyId Currency identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Central banks ordered by localized name.
     */
    fun byCurrencyId(currencyId: Long, languageCode: String?): List<CentralBankData> =
        localizedSubset(idsByCurrencyId[currencyId], languageCode)

    private fun localizedSubset(ids: List<Long>?, languageCode: String?): List<CentralBankData> {
        if (ids.isNullOrEmpty()) {
            return emptyList()
        }

        val matching = ids.mapNotNull(byId::get)
        return index.sortSubset(matching, languageCode).map { it.localized(languageCode) }
    }

    private companion object {
        /** Inverts a bank's outgoing relationship list into a lookup from the related entity. */
        inline fun reverseIndex(
            dataset: RawDataset,
            relatedIds: (dev.voir.sole.world.api.dataset.json.CentralBankJSON) -> List<Long>,
        ): Map<Long, List<Long>> {
            val index = mutableMapOf<Long, MutableList<Long>>()
            for (bank in dataset.centralBanks) {
                for (relatedId in relatedIds(bank)) {
                    index.getOrPut(relatedId) { mutableListOf() }.add(bank.id)
                }
            }

            return index
        }
    }
}

/**
 * A central bank as held in memory, keeping every translation so names resolve per request.
 * @property id Stable central bank identifier.
 * @property name Display name in every available language.
 * @property nativeName Name in the bank's own language, when available.
 * @property websiteURL Official website, when available.
 * @property establishmentYear Year the bank was established, when known.
 */
private class CentralBankRecord(
    val id: Long,
    val name: LocalizedName,
    val nativeName: String?,
    val websiteURL: String?,
    val establishmentYear: Int?,
) {
    /** Scores this central bank against a search query. */
    fun score(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)
        return maxOf(nameScore, query.score(nativeName, NATIVE_NAME_WEIGHT))
    }

    fun localized(languageCode: String?) = CentralBankData(
        id = id,
        name = name.resolve(languageCode),
        nativeName = nativeName,
        websiteURL = websiteURL,
        establishmentYear = establishmentYear,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val NATIVE_NAME_WEIGHT = 60
    }
}
