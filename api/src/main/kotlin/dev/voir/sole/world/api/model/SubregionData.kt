package dev.voir.sole.world.api.model

/**
 * API model returned for subregion records.
 * @property id Subregion alias, such as `western-europe`.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property regionId Parent region id.
 */
data class SubregionData(
    val id: String,
    val name: String,
    val regionId: String,
)
