package dev.voir.sole.world.api.crypto

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.FoldedText
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.CryptoData
import kotlinx.datetime.LocalDate
import org.springframework.stereotype.Component

/**
 * Read model for cryptocurrencies.
 *
 * Coin names are the same in every language, so nothing here is translated. Ordering still runs
 * through [LocalizedIndex] so that a caller asking for a language gets the collation a reader of
 * that language expects — "Ä" lands where a German reader looks for it whether or not the name
 * itself changed.
 */
@Component
class CryptoStore(dataset: RawDataset) {
    private val records: List<CryptoRecord> = dataset.cryptos.map { json ->
        CryptoRecord(
            id = json.id,
            code = json.code,
            name = json.name,
            description = json.description,
            websiteUrl = json.websiteUrl,
            introducedYear = json.introducedYear,
            decimalDigits = json.decimalDigits,
            obsolete = json.obsolete,
            obsoleteAt = parseDate(json.obsoleteAt, json.id),
            logoId = json.logoId,
        )
    }

    private val byId: IdIndex<CryptoRecord> = IdIndex.of(records) { it.id }

    private val byCode: IdIndex<CryptoRecord> = IdIndex.of(records) { it.code }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, _ -> record.name },
        tieBreaker = { it.id },
    )

    /** Total number of cryptocurrencies. */
    val size: Int get() = records.size

    /**
     * Loads one cryptocurrency.
     * @param id Cryptocurrency identifier.
     * @return Matching coin, or null when none exists.
     */
    fun byId(id: String): CryptoData? = byId[id]?.toData()

    /**
     * Loads several cryptocurrencies, skipping identifiers that do not exist.
     * @param ids Cryptocurrency identifiers.
     * @return Matching coins in request order.
     */
    fun byIds(ids: List<String>): List<CryptoData> = ids.mapNotNull { byId[it]?.toData() }

    /**
     * Resolves a cryptocurrency from its identifier or its ticker.
     *
     * The identifier wins: a ticker can be reassigned, and one coin's ticker may spell another
     * coin's alias.
     *
     * @param identifier Alias or ticker, in any case.
     * @return Matching coin, or null when nothing matches.
     */
    fun resolve(identifier: String): CryptoData? = (byId[identifier] ?: byCode[identifier])?.toData()

    /**
     * Returns one page of cryptocurrencies ordered by name.
     * @param request Normalized page request.
     * @param languageCode Internal language code, which selects the collation order.
     * @param query Relevance search, or null to list.
     * @param obsolete Restricts results to obsolete or active coins; null returns both.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
        obsolete: Boolean? = null,
    ): Page<CryptoData> {
        val candidates = if (obsolete == null) records else records.filter { it.obsolete == obsolete }

        val ordered = if (query == null) {
            index.sortSubset(candidates, languageCode)
        } else {
            Ranking.rank(candidates, query, CryptoRecord::score) { it.id }
        }

        val page = ordered.toPage(request)

        return Page(items = page.items.map { it.toData() }, metadata = page.metadata)
    }

    private companion object {
        /**
         * Parses an optional date from the dataset.
         * @param value Raw date string, or null when the record carries none.
         * @param alias Coin the value belongs to, named in the failure so the file can be fixed.
         * @return Parsed date, or null when there was none.
         */
        fun parseDate(value: String?, alias: String): LocalDate? {
            val raw = value?.trim()?.ifBlank { null } ?: return null

            return try {
                LocalDate.parse(raw)
            } catch (failure: IllegalArgumentException) {
                throw IllegalStateException(
                    "Cryptocurrency '$alias' has an unparsable obsoleteAt: $raw",
                    failure,
                )
            }
        }
    }
}

/**
 * A cryptocurrency as held in memory, with its searchable text folded once.
 * @property id The coin's alias: its stable lowercase key.
 * @property code Ticker symbol the coin trades under.
 * @property name Display name.
 * @property description Description text, when available.
 * @property websiteUrl Official project website, when known.
 * @property introducedYear Year the coin launched, when known.
 * @property decimalDigits Number of decimal digits used for minor units.
 * @property obsolete Whether the coin is no longer active.
 * @property obsoleteAt Date the coin became obsolete, when known.
 * @property logoId Media asset id of the coin's logo, when one is bundled.
 */
private class CryptoRecord(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val websiteUrl: String?,
    val introducedYear: Int?,
    val decimalDigits: Int,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val logoId: String?,
) {
    private val foldedName = FoldedText.of(name)

    private val foldedCode = FoldedText.of(code)

    private val foldedId = FoldedText.of(id)

    /**
     * Scores this coin against a search query.
     *
     * The ticker and the alias the coin is identified by rank just below the name: they are what a
     * caller is most likely to type exactly, but a coin is identified by its name when both match.
     */
    fun score(query: SearchQuery): Int = maxOf(
        query.score(foldedName, NAME_WEIGHT),
        query.score(foldedCode, CODE_WEIGHT),
        query.score(foldedId, ID_WEIGHT),
    )

    fun toData() = CryptoData(
        id = id,
        code = code,
        name = name,
        description = description,
        websiteUrl = websiteUrl,
        introducedYear = introducedYear,
        decimalDigits = decimalDigits,
        obsolete = obsolete,
        obsoleteAt = obsoleteAt,
        logoId = logoId,
    )

    private companion object {
        const val NAME_WEIGHT = 100
        const val CODE_WEIGHT = 95
        const val ID_WEIGHT = 90
    }
}
