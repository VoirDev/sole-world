package dev.voir.sole.world.api.model

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
    val id: Long,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val squareAssetId: Long?,
    val wideAssetId: Long?,
)
