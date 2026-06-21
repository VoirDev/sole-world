package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a country translation row.
 * @property languageCode Locale code for the translated text.
 * @property countryId Parent country id.
 * @property name Localized display name.
 */
data class NewCountryTranslationData(
    val languageCode: String,
    val countryId: Int,
    val name: String,
)
