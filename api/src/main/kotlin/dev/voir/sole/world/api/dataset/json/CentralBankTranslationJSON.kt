package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized central bank name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized central bank name.
 */
@Serializable
data class CentralBankTranslationJSON(
    val locale: String,
    val name: String,
)
