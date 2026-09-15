package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized country name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized country name.
 */
@Serializable
data class CountryTranslationJSON(
    val languageCode: String,
    val name: String,
)
