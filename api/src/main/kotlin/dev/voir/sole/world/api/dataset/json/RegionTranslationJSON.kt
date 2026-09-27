package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized region name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized region name.
 */
@Serializable
data class RegionTranslationJSON(
    val locale: String,
    val name: String,
)
