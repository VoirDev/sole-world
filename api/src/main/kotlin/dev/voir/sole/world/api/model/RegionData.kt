package dev.voir.sole.world.api.model

/**
 * API model returned for region records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 */
data class RegionData(
    val id: Long,
    val name: String,
)
