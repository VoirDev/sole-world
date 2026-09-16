package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized central bank name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized central bank name.
 */
@Serializable
data class CentralBankTranslationJSON(
    val languageCode: String,
    val name: String,
)
