package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * State or province record nested under a country seed file.
 * @property id State or province primary key used during import.
 * @property name Default state or province name.
 * @property stateCode State or province code, when available.
 * @property latitude Latitude coordinate in decimal degrees, when available.
 * @property longitude Longitude coordinate in decimal degrees, when available.
 * @property type Administrative division type, when available.
 * @property translations Localized state or province names.
 * @property cities Cities nested under the state or province.
 */
@Serializable
internal data class StateJSON(
    val id: Long,
    val name: String,
    val stateCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
    val translations: List<StateTranslationJSON>,
    val cities: List<CityJSON>
)
