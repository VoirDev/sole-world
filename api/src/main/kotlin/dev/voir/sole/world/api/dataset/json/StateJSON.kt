package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * State or province record nested under a country seed file.
 * @property id Country alpha-2 code and state code joined by a hyphen, such as `US-CA`, or an alias of that
 * form for a state with no code.
 * @property name Default state or province name.
 * @property stateCode State or province code, when available.
 * @property latitude Latitude coordinate in decimal degrees, when available.
 * @property longitude Longitude coordinate in decimal degrees, when available.
 * @property type Administrative division type, when available.
 * @property translations Localized state or province names.
 * @property cities Cities nested under the state or province.
 */
@Serializable
data class StateJSON(
    val id: String,
    val name: String,
    val stateCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
    val translations: List<StateTranslationJSON>,
    val cities: List<CityJSON>,
)
