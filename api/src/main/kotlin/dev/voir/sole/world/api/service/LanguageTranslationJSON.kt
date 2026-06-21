package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized language name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized language name.
 */
@Serializable
internal data class LanguageTranslationJSON(
    val languageCode: String,
    val name: String
)
