package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized state or province name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized state or province name.
 */
@Serializable
internal data class StateTranslationJSON(
    val languageCode: String,
    val name: String,
)
