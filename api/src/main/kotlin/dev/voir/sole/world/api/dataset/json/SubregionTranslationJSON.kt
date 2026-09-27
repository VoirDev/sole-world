package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized subregion name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized subregion name.
 */
@Serializable
data class SubregionTranslationJSON(
    val locale: String,
    val name: String,
)
