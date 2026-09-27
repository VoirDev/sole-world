package dev.voir.sole.world.api.city

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.IdKey
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.CityData
import org.springframework.stereotype.Component

/**
 * Read model for cities, the largest entity in the dataset.
 *
 * Cities are nested two levels deep in the bundled country documents, so this store keeps the
 * country identifier alongside the state identifier. That avoids joining back through states for
 * the very common "cities in this country" lookup.
 */
@Component
class CityStore(dataset: RawDataset) {
    private val records: List<CityRecord> = dataset.countries.flatMap { country ->
        country.states.flatMap { state ->
            state.cities.map { json ->
                CityRecord(
                    id = json.id,
                    stateId = state.id,
                    countryId = country.id,
                    name = LocalizedName.of(
                        base = json.name,
                        translations = json.translations.map { it.languageCode to it.name },
                    ),
                    latitude = json.latitude,
                    longitude = json.longitude,
                )
            }
        }
    }

    private val byId: Map<Long, CityRecord> = records.associateBy { it.id }

    private val byStateId: IdIndex<List<CityRecord>> = IdIndex.grouped(records) { it.stateId }

    private val byCountryId: IdIndex<List<CityRecord>> = IdIndex.grouped(records) { it.countryId }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = CityRecord::id,
    )

    /** Total number of cities. */
    val size: Int get() = records.size

    /**
     * Loads one city.
     * @param id City identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching city, or null when none exists.
     */
    fun byId(id: Long, languageCode: String?): CityData? = byId[id]?.localized(languageCode)

    /**
     * Loads several cities, skipping identifiers that do not exist.
     * @param ids City identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching cities in request order.
     */
    fun byIds(ids: List<Long>, languageCode: String?): List<CityData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of cities, optionally restricted to a country or state, or searched.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @param countryId Restrict to cities in this country.
     * @param stateId Restrict to cities in this state.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        countryId: String? = null,
        stateId: String? = null,
    ): Page<CityData> {
        // Narrow by the most selective relationship first so a search scans as little as possible.
        var candidates = when {
            stateId != null -> byStateId[stateId].orEmpty()
            countryId != null -> byCountryId[countryId].orEmpty()
            else -> records
        }

        if (stateId != null && countryId != null) {
            val country = IdKey.of(countryId)
            candidates = candidates.filter { IdKey.of(it.countryId) == country }
        }

        val ordered = if (query == null) {
            index.sortSubset(candidates, languageCode)
        } else {
            Ranking.rank(candidates, query, CityRecord::score) { it.id }
        }

        return localizedPage(ordered, request, languageCode)
    }

    /**
     * Returns one page of a country's cities, optionally narrowed by name.
     * @param countryId Country identifier.
     * @param request Normalized page request.
     * @param query Literal name filter, or null for every city in the country.
     * @param languageCode Internal language code, or null for base data.
     * @return Requested page.
     */
    fun pageByCountryId(
        countryId: String,
        request: PageRequest,
        query: SearchQuery?,
        languageCode: String?,
    ): Page<CityData> = pageOf(byCountryId[countryId], request, query, languageCode)

    /**
     * Returns one page of a state's cities, optionally narrowed by name.
     * @param stateId State identifier.
     * @param request Normalized page request.
     * @param query Literal name filter, or null for every city in the state.
     * @param languageCode Internal language code, or null for base data.
     * @return Requested page.
     */
    fun pageByStateId(
        stateId: String,
        request: PageRequest,
        query: SearchQuery?,
        languageCode: String?,
    ): Page<CityData> = pageOf(byStateId[stateId], request, query, languageCode)

    private fun pageOf(
        candidates: List<CityRecord>?,
        request: PageRequest,
        query: SearchQuery?,
        languageCode: String?,
    ): Page<CityData> {
        val matching = candidates.orEmpty().filter { query == null || it.matches(query) }
        return localizedPage(index.sortSubset(matching, languageCode), request, languageCode)
    }

    private fun localizedPage(
        ordered: List<CityRecord>,
        request: PageRequest,
        languageCode: String?,
    ): Page<CityData> {
        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }
}

/**
 * A city as held in memory, keeping every translation so names resolve per request.
 * @property id Stable city identifier.
 * @property stateId Parent state identifier.
 * @property countryId Country the parent state belongs to.
 * @property name Display name in every available language.
 * @property latitude Latitude in decimal degrees.
 * @property longitude Longitude in decimal degrees.
 */
private class CityRecord(
    val id: Long,
    val stateId: String,
    val countryId: String,
    val name: LocalizedName,
    val latitude: Double,
    val longitude: Double,
) {
    /** Tests this city against a literal name filter, in any language it is known by. */
    fun matches(query: SearchQuery): Boolean = name.matches(query)

    /** Scores this city against a search query. */
    fun score(query: SearchQuery): Int = name.score(query, NAME_WEIGHT)

    fun localized(languageCode: String?) = CityData(
        id = id,
        stateId = stateId,
        name = name.resolve(languageCode),
        latitude = latitude,
        longitude = longitude,
    )

    private companion object {
        const val NAME_WEIGHT = 100
    }
}
