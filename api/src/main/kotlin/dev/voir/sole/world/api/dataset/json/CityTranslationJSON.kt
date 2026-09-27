package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized city name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized city name.
 */
@Serializable
data class CityTranslationJSON(
    val locale: String,
    val name: String,
)
