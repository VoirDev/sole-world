package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Metadata record describing the bundled seed data version.
 * @property version Seed data version number.
 * @property date Seed data release date string.
 */
@Serializable
data class MetaJSON(
    val version: Int,
    val date: String,
)
