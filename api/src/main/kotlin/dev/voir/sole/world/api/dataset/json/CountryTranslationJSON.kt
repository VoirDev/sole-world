package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized country name read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property name Localized country name.
 * @property aliases Other names the country is known by in this locale; omitted when there are none.
 */
@Serializable
data class CountryTranslationJSON(
    val locale: String,
    val name: String,
    val aliases: List<String> = emptyList(),
)
