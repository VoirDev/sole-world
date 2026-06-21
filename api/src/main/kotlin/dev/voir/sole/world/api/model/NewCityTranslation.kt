package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a city translation row.
 * @property cityId cityId value.
 * @property languageCode Locale code for the translated text.
 * @property name Localized display name.
 */
data class NewCityTranslation(
    val cityId: Long,
    val languageCode: String,
    val name: String
)
