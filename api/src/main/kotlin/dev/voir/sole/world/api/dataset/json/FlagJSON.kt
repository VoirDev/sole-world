package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Shared flag record read from bundled seed data.
 * @property id Flag primary key used during import.
 * @property caption Human-readable flag caption.
 * @property emoji Flag emoji.
 * @property emojiU Unicode codepoint form of the flag emoji.
 * @property square Square media asset id, when available.
 * @property wide Wide media asset id, when available.
 */
@Serializable
data class FlagJSON(
    val id: Long,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val square: Long?,
    val wide: Long?,
)
