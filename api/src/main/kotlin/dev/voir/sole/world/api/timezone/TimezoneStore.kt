package dev.voir.sole.world.api.timezone

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.TimezoneData
import org.springframework.stereotype.Component

/** Read model for timezones and their country relationships. */
@Component
class TimezoneStore(dataset: RawDataset) {
    private val records: List<TimezoneRecord> = dataset.timezones.map { json ->
        TimezoneRecord(
            id = json.id,
            zoneName = json.zoneName,
            tzName = LocalizedName.of(
                base = json.tzName,
                translations = json.translations.map { it.languageCode to it.tzName },
            ),
            abbreviation = json.abbreviation,
            gmtOffset = json.gmtOffset,
            gmtOffsetName = json.gmtOffsetName,
        )
    }

    private val byId: Map<Long, TimezoneRecord> = records.associateBy { it.id }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.tzName.resolve(language) },
        tieBreaker = TimezoneRecord::id,
    )

    private val idsByCountryId: Map<Long, List<Long>> = dataset.countries
        .associate { country -> country.id to country.timezoneIds }

    /** Total number of timezones. */
    val size: Int get() = records.size

    /**
     * Loads one timezone.
     * @param id Timezone identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching timezone, or null when none exists.
     */
    fun byId(id: Long, languageCode: String?): TimezoneData? = byId[id]?.localized(languageCode)

    /**
     * Loads several timezones, skipping identifiers that do not exist.
     * @param ids Timezone identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching timezones in request order.
     */
    fun byIds(ids: List<Long>, languageCode: String?): List<TimezoneData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of timezones, ordered by localized name or by relevance when searching.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<TimezoneData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, TimezoneRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the timezones associated with a country.
     * @param countryId Country identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Timezones ordered by localized name.
     */
    fun byCountryId(countryId: Long, languageCode: String?): List<TimezoneData> {
        val ids = idsByCountryId[countryId].orEmpty()
        if (ids.isEmpty()) {
            return emptyList()
        }

        val matching = ids.mapNotNull(byId::get)
        return index.sortSubset(matching, languageCode).map { it.localized(languageCode) }
    }
}

/**
 * A timezone as held in memory, keeping every translation so names resolve per request.
 * @property id Stable timezone identifier.
 * @property zoneName IANA timezone name.
 * @property tzName Display name in every available language.
 * @property abbreviation Timezone abbreviation.
 * @property gmtOffset Offset from GMT in seconds.
 * @property gmtOffsetName Human-readable GMT offset.
 */
private class TimezoneRecord(
    val id: Long,
    val zoneName: String,
    val tzName: LocalizedName,
    val abbreviation: String,
    val gmtOffset: Int,
    val gmtOffsetName: String,
) {
    /** Scores this timezone against a search query. */
    fun score(query: SearchQuery): Int {
        val nameScore = tzName.score(query, NAME_WEIGHT)

        return maxOf(
            nameScore,
            query.score(zoneName, ZONE_NAME_WEIGHT),
            query.score(abbreviation, ABBREVIATION_WEIGHT),
        )
    }

    fun localized(languageCode: String?) = TimezoneData(
        id = id,
        zoneName = zoneName,
        tzName = tzName.resolve(languageCode),
        abbreviation = abbreviation,
        gmtOffset = gmtOffset,
        gmtOffsetName = gmtOffsetName,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val ZONE_NAME_WEIGHT = 90
        const val ABBREVIATION_WEIGHT = 70
    }
}
