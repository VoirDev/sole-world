package dev.voir.sole.world.api.model

/**
 * API model returned for city records.
 * @property id Primary key for the record.
 * @property stateId Parent state or province id.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 */
data class CityData(
    val id: Long,
    val stateId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
