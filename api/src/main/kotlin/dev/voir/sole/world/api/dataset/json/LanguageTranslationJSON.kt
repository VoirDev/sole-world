package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized language name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized language name.
 */
@Serializable
data class LanguageTranslationJSON(
    val locale: String,
    val name: String,
    val description: String? = null,
)
