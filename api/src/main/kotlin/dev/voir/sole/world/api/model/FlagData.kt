package dev.voir.sole.world.api.model

/**
 * API model returned for shared flag records.
 * @property id Flag alias. A territory's flag is its lowercase ISO 3166-1 alpha-2 code, such as `us`.
 * @property caption Human-readable flag caption.
 * @property emoji Flag emoji.
 * @property emojiU Unicode codepoint form of the flag emoji.
 * @property squareAssetId Square media asset id, when available.
 * @property wideAssetId Wide media asset id, when available.
 */
data class FlagData(
    val id: String,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val squareAssetId: String?,
    val wideAssetId: String?,
)
