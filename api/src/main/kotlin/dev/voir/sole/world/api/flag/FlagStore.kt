package dev.voir.sole.world.api.flag

import dev.voir.sole.world.api.dataset.RawDataset
import dev.voir.sole.world.api.dataset.index.FoldedText
import dev.voir.sole.world.api.dataset.index.IdIndex
import dev.voir.sole.world.api.dataset.index.LocalizedIndex
import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.dataset.index.Ranking
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.model.FlagData
import org.springframework.stereotype.Component

/**
 * Read model for flag records.
 *
 * Captions are the same in every language, so nothing here is translated. Ordering still runs
 * through [LocalizedIndex] so that a caller asking for a language gets the collation a reader of
 * that language expects, and so flags behave like every other browsable collection.
 */
@Component
class FlagStore(dataset: RawDataset) {
    private val records: List<FlagRecord> = dataset.flags.map { json ->
        FlagRecord(
            data = FlagData(
                id = json.id,
                caption = json.caption,
                emoji = json.emoji,
                emojiU = json.emojiU,
                squareAssetId = json.square,
                wideAssetId = json.wide,
            ),
        )
    }

    private val byId: IdIndex<FlagRecord> = IdIndex.of(records) { it.data.id }

    private val index = LocalizedIndex(
        records = records,
        displayName = { record, _ -> record.data.caption },
        tieBreaker = { it.data.id },
    )

    /** Total number of flags. */
    val size: Int get() = records.size

    /**
     * Loads one flag.
     * @param id Flag identifier.
     * @return Matching flag, or null when none exists.
     */
    fun byId(id: String): FlagData? = byId[id]?.data

    /**
     * Loads several flags, skipping identifiers that do not exist.
     * @param ids Flag identifiers.
     * @return Matching flags in request order.
     */
    fun byIds(ids: List<String>): List<FlagData> = ids.mapNotNull { byId[it]?.data }

    /**
     * Returns one page of flags ordered by caption.
     * @param request Normalized page request.
     * @param languageCode Internal language code, which selects the collation order.
     * @param query Relevance search over captions and emoji, or null to list.
     * @return Requested page.
     */
    fun page(
        request: PageRequest,
        languageCode: String?,
        query: SearchQuery? = null,
    ): Page<FlagData> {
        val ordered = if (query == null) {
            index.sorted(languageCode)
        } else {
            Ranking.rank(records, query, FlagRecord::score) { it.data.id }
        }

        val page = ordered.toPage(request)

        return Page(items = page.items.map { it.data }, metadata = page.metadata)
    }
}

/**
 * A flag as held in memory, with its searchable text folded once.
 * @property data The published flag record.
 */
private class FlagRecord(val data: FlagData) {
    private val foldedCaption = FoldedText.of(data.caption)

    private val foldedEmoji = FoldedText.of(data.emoji)

    /**
     * Scores this flag against a search query.
     *
     * The emoji is searchable because it is the one thing a caller is likely to have in hand and
     * unable to name — pasting it in is how you find out which flag it is.
     */
    fun score(query: SearchQuery): Int = maxOf(
        query.score(foldedCaption, CAPTION_WEIGHT),
        query.score(foldedEmoji, EMOJI_WEIGHT),
    )

    private companion object {
        const val CAPTION_WEIGHT = 100
        const val EMOJI_WEIGHT = 95
    }
}
