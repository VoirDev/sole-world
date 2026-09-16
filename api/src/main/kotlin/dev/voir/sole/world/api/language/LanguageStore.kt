package dev.voir.sole.world.api.language

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.LocalizedName
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.LanguageData
import org.springframework.stereotype.Component

/** Read model for languages and their country relationships. */
@Component
class LanguageStore(dataset: RawDataset) {
    private val records: List<LanguageRecord> = dataset.languages.map { json ->
        LanguageRecord(
            id = json.id,
            code = json.code,
            nativeName = json.nativeName,
            name = LocalizedName.of(
                base = json.name,
                translations = json.translations.map { it.languageCode to it.name },
            ),
            description = json.description,
            descriptionTranslations = json.translations
                .mapNotNull { translation -> translation.description?.let { translation.languageCode to it } }
                .toMap(),
            flagId = json.flagId,
        )
    }

    private val byId: Map<Long, LanguageRecord> = records.associateBy { it.id }

    private val byCode: Map<String, LanguageRecord> = records.associateBy { it.code.lowercase() }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, language -> record.name.resolve(language) },
        tieBreaker = { it.id },
    )

    private val idsByCountryId: Map<Long, List<Long>> = dataset.countries.associate { country ->
        // Official and other languages are both "languages of the country" to a reader.
        country.id to (country.officialLanguageIds + country.otherLanguageIds).distinct()
    }

    /** Total number of languages. */
    val size: Int get() = records.size

    /**
     * Loads one language.
     * @param id Language identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching language, or null when none exists.
     */
    fun byId(id: Long, languageCode: String?): LanguageData? = byId[id]?.localized(languageCode)

    /**
     * Loads one language by its identifier or its language code.
     * @param identifier Numeric identifier or language code such as "fr".
     * @param languageCode Internal language code, or null for base data.
     * @return Matching language, or null when none exists.
     */
    fun byIdentifier(identifier: String, languageCode: String?): LanguageData? {
        val record = identifier.toLongOrNull()?.let(byId::get) ?: byCode[identifier.lowercase()]
        return record?.localized(languageCode)
    }

    /**
     * Loads several languages, skipping identifiers that do not exist.
     * @param ids Language identifiers.
     * @param languageCode Internal language code, or null for base data.
     * @return Matching languages in request order.
     */
    fun byIds(ids: List<Long>, languageCode: String?): List<LanguageData> =
        ids.mapNotNull { byId[it]?.localized(languageCode) }

    /**
     * Returns one page of languages, ordered by localized name or by relevance when searching.
     * @param request Normalized page request.
     * @param languageCode Internal language code, or null for base data.
     * @param query Relevance search, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<LanguageData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, LanguageRecord::score) { it.id }
        }

        val page = ordered.toPage(request)
        return Page(
            items = page.items.map { it.localized(languageCode) },
            metadata = page.metadata,
        )
    }

    /**
     * Loads the languages associated with a country, official and otherwise.
     * @param countryId Country identifier.
     * @param languageCode Internal language code, or null for base data.
     * @return Languages ordered by localized name.
     */
    fun byCountryId(countryId: Long, languageCode: String?): List<LanguageData> {
        val ids = idsByCountryId[countryId].orEmpty()
        if (ids.isEmpty()) {
            return emptyList()
        }

        val matching = ids.mapNotNull(byId::get)
        return index.sortSubset(matching, languageCode).map { it.localized(languageCode) }
    }
}

/**
 * A language as held in memory, keeping every translation so names resolve per request.
 * @property id Stable language identifier.
 * @property code Language code.
 * @property nativeName Name in the language's own script, when available.
 * @property name Display name in every available language.
 * @property description Base description, when available.
 * @property descriptionTranslations Translated descriptions by internal language code.
 * @property flagId Shared flag identifier, when available.
 */
private class LanguageRecord(
    val id: Long,
    val code: String,
    val nativeName: String?,
    val name: LocalizedName,
    val description: String?,
    val descriptionTranslations: Map<String, String>,
    val flagId: Long?,
) {
    /** Scores this language against a search query. */
    fun score(query: SearchQuery): Int {
        val nameScore = name.score(query, NAME_WEIGHT)

        return maxOf(
            nameScore,
            query.score(code, CODE_WEIGHT),
            query.score(nativeName, NATIVE_NAME_WEIGHT),
        )
    }

    fun localized(languageCode: String?) = LanguageData(
        id = id,
        code = code,
        nativeName = nativeName,
        name = name.resolve(languageCode),
        description = languageCode?.let { descriptionTranslations[it] } ?: description,
        flagId = flagId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val CODE_WEIGHT = 80
        const val NATIVE_NAME_WEIGHT = 60
    }
}
