package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized region name read from seed data.
 * @property languageCode Locale code for the translation.
 * @property name Localized region name.
 */
@Serializable
data class RegionTranslationJSON(
    val languageCode: String,
    val name: String,
)
