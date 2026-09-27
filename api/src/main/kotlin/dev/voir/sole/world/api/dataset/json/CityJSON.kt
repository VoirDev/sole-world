package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * City record nested under a state in the bundled country seed data.
 * @property id Numeric city identifier. Cities have no public code, so they keep their numbers.
 * @property name Default city name.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property translations Localized city names.
 */
@Serializable
data class CityJSON(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val translations: List<CityTranslationJSON>,
)
