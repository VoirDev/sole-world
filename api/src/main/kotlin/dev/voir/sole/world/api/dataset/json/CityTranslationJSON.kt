package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized city name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized city name.
 */
@Serializable
data class CityTranslationJSON(
    val languageCode: String,
    val name: String,
)
