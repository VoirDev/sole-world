package dev.voir.sole.world.api.subregion

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.SubregionData
import org.springframework.stereotype.Component

/** Read model for subregions, which are nested inside regions in the bundled dataset. */
@Component
class SubregionStore(dataset: RawDataset) {
    private val records: List<SubregionRecord> = dataset.regions.flatMap { region ->
        region.subregions.map { json ->
            SubregionRecord(
                id = json.id,
                regionId = region.id,
                name = LocalizedName.of(
                    base = json.name,
                    translations = json.translations.map { it.languageCode to it.name },
                ),
            )
        }
    }

    private val byId: IdIndex<SubregionRecord> = IdIndex.of(records) { it.id }

    private val byRegionId: IdIndex<List<SubregionRecord>> = IdIndex.grouped(records) { it.regionId }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    /** Total number of subregions. */
    val size: Int get() = records.size

    /**
     * Loads one subregion.
     * @param id Subregion identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching subregion, or null when none exists.
     */
    fun byId(id: String, languageCode: String?): SubregionData? = byId[id]?.localized(languageCode)

    /**
     * Loads several subregions, skipping identifiers that do not exist.
     * @param ids Subregion identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching subregions in request order.
     */
    fun byIds(ids: List<String>, languageCode: String?): List<SubregionData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of subregions, optionally restricted to a region or searched.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @param regionId Restrict to subregions inside this region.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        regionId: String? = null,
    ): Page<SubregionData> {
        val candidates = if (regionId == null) records else byRegionId[regionId].orEmpty()

        val ordered = if (query == null) {
            index.sortSubset(candidates, languageCode)
        } else {
            Ranking.rank(candidates, query, SubregionRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the subregions inside a region.
     * @param regionId Region identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Subregions ordered by localized name.
     */
    fun byRegionId(regionId: String, languageCode: String?): List<SubregionData> {
        val matching = byRegionId[regionId].orEmpty()
        if (matching.isEmpty()) {
            return emptyList()
        }

        return index.sortSubset(matching, languageCode).map { it.localized(languageCode) }
    }
}

/**
 * A subregion as held in memory, keeping every translation so names resolve per request.
 * @property id Stable subregion identifier.
 * @property regionId Parent region identifier.
 * @property name Display name in every available language.
 */
private class SubregionRecord(
    val id: String,
    val regionId: String,
    val name: LocalizedName,
) {
    /** Scores this subregion against a search query. */
    fun score(query: SearchQuery): Int = name.score(query, NAME_WEIGHT)

    fun localized(languageCode: String?) = SubregionData(
        id = id,
        name = name.resolve(languageCode),
        regionId = regionId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
    }
}
