package dev.voir.sole.world.api.model

/**
 * API model returned for subregion records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property regionId Parent region id.
 */
data class SubregionData(
    val id: Long,
    val name: String,
    val regionId: Long,
)
