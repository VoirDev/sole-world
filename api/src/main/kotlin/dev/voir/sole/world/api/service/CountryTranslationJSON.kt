package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized country name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized country name.
 */
@Serializable
internal data class CountryTranslationJSON(
    val languageCode: String,
    val name: String
)
