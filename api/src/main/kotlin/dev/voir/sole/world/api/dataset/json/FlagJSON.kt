package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Shared flag record read from bundled seed data.
 * @property id Flag alias. A territory's flag is its lowercase ISO 3166-1 alpha-2 code, such as `us`.
 * @property caption Human-readable flag caption.
 * @property emoji Flag emoji.
 * @property emojiU Unicode codepoint form of the flag emoji.
 * @property square Square media asset id, when available.
 * @property wide Wide media asset id, when available.
 */
@Serializable
data class FlagJSON(
    val id: String,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val square: String?,
    val wide: String?,
)
