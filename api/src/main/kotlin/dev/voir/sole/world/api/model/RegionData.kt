package dev.voir.sole.world.api.model

/**
 * API model returned for region records.
 * @property id Region alias, such as `europe`.
 * @property name Display name, localized by mapper functions when a language is requested.
 */
data class RegionData(
    val id: String,
    val name: String,
)
