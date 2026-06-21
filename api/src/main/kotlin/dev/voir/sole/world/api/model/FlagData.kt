package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.FlagsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for shared flag records.
 * @property id Primary key for the flag.
 * @property caption Human-readable flag caption.
 * @property emoji Flag emoji.
 * @property emojiU Unicode codepoint form of the flag emoji.
 * @property squareAssetId Square media asset id, when available.
 * @property wideAssetId Wide media asset id, when available.
 */
data class FlagData(
    val id: Int,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val squareAssetId: Long?,
    val wideAssetId: Long?,
) {
    companion object {
        /**
         * Maps an Exposed result row to a flag API model.
         * @return Flag API model populated from this row.
         */
        fun ResultRow.toFlag() = FlagData(
            id = this[FlagsTable.id].value,
            caption = this[FlagsTable.caption],
            emoji = this[FlagsTable.emoji],
            emojiU = this[FlagsTable.emojiU],
            squareAssetId = this[FlagsTable.squareAsset]?.value,
            wideAssetId = this[FlagsTable.wideAsset]?.value,
        )
    }
}
