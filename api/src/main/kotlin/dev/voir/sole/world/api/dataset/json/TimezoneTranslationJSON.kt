package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Localized timezone labels read from seed data.
 * @property locale Locale the translation is written in, an id from `locales.json`.
 * @property tzName Localized timezone display name.
 */
@Serializable
data class TimezoneTranslationJSON(
    val locale: String,
    val tzName: String,
)
