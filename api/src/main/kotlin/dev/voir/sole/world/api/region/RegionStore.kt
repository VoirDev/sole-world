package dev.voir.sole.world.api.region

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.RegionData
import org.springframework.stereotype.Component

/** Read model for top-level geographic regions. */
@Component
class RegionStore(dataset: RawDataset) {
    private val records: List<RegionRecord> = dataset.regions.map { json ->
        RegionRecord(
            id = json.id,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.languageCode to it.name },
            ),
        )
    }

    private val byId: IdIndex<RegionRecord> = IdIndex.of(records) { it.id }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    /** Total number of regions. */
    val size: Int get() = records.size

    /**
     * Loads one region.
     * @param id Region identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching region, or null when none exists.
     */
    fun byId(id: String, languageCode: String?): RegionData? = byId[id]?.localized(languageCode)

    /**
     * Loads several regions, skipping identifiers that do not exist.
     * @param ids Region identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching regions in request order.
     */
    fun byIds(ids: List<String>, languageCode: String?): List<RegionData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of regions, ordered by localized name or by relevance when searching.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<RegionData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, RegionRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }
}

/**
 * A region as held in memory, keeping every translation so names resolve per request.
 * @property id Stable region identifier.
 * @property name Display name in every available language.
 */
private class RegionRecord(
    val id: String,
    val name: LocalizedName,
) {
    /** Scores this region against a search query. */
    fun score(query: SearchQuery): Int = name.score(query, NAME_WEIGHT)

    fun localized(languageCode: String?) = RegionData(
        id = id,
        name = name.resolve(languageCode),
    )

    private companion object {
        const val NAME_WEIGHT = 100
    }
}
