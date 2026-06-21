package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Localized timezone labels read from seed data.
 * @property languageCode Locale code for the translation.
 * @property tzName Localized timezone display name.
 */
@Serializable
internal data class TimezoneTranslationJSON(
    val languageCode: String,
    val tzName: String
)
