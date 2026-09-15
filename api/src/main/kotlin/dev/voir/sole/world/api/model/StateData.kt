package dev.voir.sole.world.api.model

/**
 * API model returned for state or province records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property stateCode State or province code, when available.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property type Administrative division type, when available.
 * @property countryId Parent country id.
 */
data class StateData(
    val id: Long,
    val name: String,
    val stateCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
    val countryId: Long,
)
