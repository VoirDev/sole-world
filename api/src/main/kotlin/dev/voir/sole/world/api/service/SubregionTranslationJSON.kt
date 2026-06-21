package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized subregion name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized subregion name.
 */
@Serializable
internal data class SubregionTranslationJSON(
    val languageCode: String,
    val name: String,
)
