package dev.voir.sole.world.api.state

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.StateData
import org.springframework.stereotype.Component

/** Read model for first-level administrative divisions, nested under countries in the dataset. */
@Component
class StateStore(dataset: RawDataset) {
    private val records: List<StateRecord> = dataset.countries.flatMap { country ->
        country.states.map { json ->
            StateRecord(
                id = json.id,
                countryId = country.id,
                name = LocalizedName.of(
                    base = json.name,
                    translations = json.translations.map { it.languageCode to it.name },
                ),
                stateCode = json.stateCode,
                latitude = json.latitude,
                longitude = json.longitude,
                type = json.type,
            )
        }
    }

    private val byId: IdIndex<StateRecord> = IdIndex.of(records) { it.id }

    private val byCountryId: IdIndex<List<StateRecord>> = IdIndex.grouped(records) { it.countryId }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = StateRecord::id,
    )

    /** Total number of states. */
    val size: Int get() = records.size

    /**
     * Loads one state.
     * @param id State identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching state, or null when none exists.
     */
    fun byId(id: String, languageCode: String?): StateData? = byId[id]?.localized(languageCode)

    /**
     * Loads several states, skipping identifiers that do not exist.
     * @param ids State identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching states in request order.
     */
    fun byIds(ids: List<String>, languageCode: String?): List<StateData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of states, optionally restricted to a country or searched.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @param countryId Restrict to states in this country.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        countryId: String? = null,
    ): Page<StateData> {
        val candidates = if (countryId == null) records else byCountryId[countryId].orEmpty()

        val ordered = if (query == null) {
            index.sortSubset(candidates, languageCode)
        } else {
            Ranking.rank(candidates, query, StateRecord::score) { it.id }
        }

        return localizedPage(ordered, request, languageCode)
    }

    /**
     * Returns one page of a country's states, optionally narrowed by name.
     * @param countryId Country identifier.
     * @param request Normalized page request.
     * @param query Literal name filter, or null for every state in the country.
     * @param languageCode Internal language code, or null for base data.
     * @return Requested page.
     */
    fun pageByCountryId(
        countryId: String,
        request: PageRequest,
        query: SearchQuery?,
        languageCode: String?,
    ): Page<StateData> {
        val matching = byCountryId[countryId].orEmpty().filter { query == null || it.matches(query) }
        return localizedPage(index.sortSubset(matching, languageCode), request, languageCode)
    }

    private fun localizedPage(
        ordered: List<StateRecord>,
        request: PageRequest,
        languageCode: String?,
    ): Page<StateData> {
        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }
}

/**
 * A state as held in memory, keeping every translation so names resolve per request.
 * @property id Stable state identifier.
 * @property countryId Parent country identifier.
 * @property name Display name in every available language.
 * @property stateCode Administrative division code, when available.
 * @property latitude Latitude in decimal degrees, when available.
 * @property longitude Longitude in decimal degrees, when available.
 * @property type Administrative division type, when available.
 */
private class StateRecord(
    val id: String,
    val countryId: String,
    val name: LocalizedName,
    val stateCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
) {
    /** Tests this state against a literal name filter, in any language it is known by. */
    fun matches(query: SearchQuery): Boolean = name.matches(query)

    /** Scores this state against a search query. */
    fun score(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)
        return maxOf(nameScore, query.score(stateCode, STATE_CODE_WEIGHT))
    }

    fun localized(languageCode: String?) = StateData(
        id = id,
        name = name.resolve(languageCode),
        stateCode = stateCode,
        latitude = latitude,
        longitude = longitude,
        type = type,
        countryId = countryId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val STATE_CODE_WEIGHT = 70
    }
}
