package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized state or province name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized state or province name.
 */
@Serializable
data class StateTranslationJSON(
    val locale: String,
    val name: String,
)
