package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Metadata record describing the bundled seed data version.
 * @property version Seed data version number.
 * @property date Seed data release date string.
 */
@Serializable
internal data class MetaJSON(
    val version: Int,
    val date: String
)
