package dev.voir.sole.world.api.service

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
internal data class FlagJSON(
    val id: Int,
    val caption: String,
    val emoji: String,
    val emojiU: String,
    val square: Long?,
    val wide: Long?,
)
