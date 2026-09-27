package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized country name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized country name.
 */
@Serializable
data class CountryTranslationJSON(
    val locale: String,
    val name: String,
)
