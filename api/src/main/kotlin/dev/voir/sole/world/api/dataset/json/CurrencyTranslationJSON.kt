package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized currency name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized currency name.
 */
@Serializable
data class CurrencyTranslationJSON(
    val languageCode: String,
    val name: String,
    val description: String? = null,
)
