package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized city name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized city name.
 */
@Serializable
internal data class CityTranslationJSON(
    val languageCode: String,
    val name: String,
)
