package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.LocaleData
import org.springframework.stereotype.Component

/** Read model for the translation locales this deployment serves. */
@Component
class LocaleStore(dataset: RawDataset) {
    private val records: List<LocaleRecord> = dataset.locales.map { json ->
        LocaleRecord(
            id = json.id,
            languageId = json.languageId,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.locale to it.name },
            ),
            nativeName = json.nativeName,
        )
    }

    private val byId: IdIndex<LocaleRecord> = IdIndex.of(records) { it.id }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    /** Matches callers' language preferences to these locales. */
    val negotiation = LocaleNegotiation(dataset.locales)

    /** Total number of locales. */
    val size: Int get() = records.size

    /**
     * Loads one locale.
     * @param id Locale identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching locale, or null when none exists.
     */
    fun byId(id: String, languageCode: String?): LocaleData? = byId[id]?.localized(languageCode)

    /**
     * Loads several locales, skipping identifiers that do not exist.
     * @param ids Locale identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching locales in request order.
     */
    fun byIds(ids: List<String>, languageCode: String?): List<LocaleData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of locales, ordered by localized name or by relevance when searching.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<LocaleData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, LocaleRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }
}

/**
 * A locale as held in memory, keeping every translation so names resolve per request.
 * @property id BCP 47 language tag.
 * @property languageId Language the locale is a form of.
 * @property name Display name in every available language.
 * @property nativeName Name written in the locale itself.
 */
private class LocaleRecord(
    val id: String,
    val languageId: String,
    val name: LocalizedName,
    val nativeName: String,
) {
    /** Scores this locale against a search query. */
    fun score(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)

        return maxOf(
            nameScore,
            query.score(id, CODE_WEIGHT),
            query.score(nativeName, NATIVE_NAME_WEIGHT),
        )
    }

    fun localized(languageCode: String?) = LocaleData(
        id = id,
        languageId = languageId,
        name = name.resolve(languageCode),
        nativeName = nativeName,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val CODE_WEIGHT = 80
        const val NATIVE_NAME_WEIGHT = 60
    }
}
