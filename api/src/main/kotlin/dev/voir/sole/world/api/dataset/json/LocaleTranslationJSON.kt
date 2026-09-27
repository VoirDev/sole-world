package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized locale name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized locale name.
 */
@Serializable
data class LocaleTranslationJSON(
    val locale: String,
    val name: String,
)
