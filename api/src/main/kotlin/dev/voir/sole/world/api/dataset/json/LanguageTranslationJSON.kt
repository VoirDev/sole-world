package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized language name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized language name.
 */
@Serializable
data class LanguageTranslationJSON(
    val languageCode: String,
    val name: String,
    val description: String? = null,
)
