package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized currency name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized currency name.
 */
@Serializable
data class CurrencyTranslationJSON(
    val locale: String,
    val name: String,
    val description: String? = null,
)
