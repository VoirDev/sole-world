package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a state or province translation row.
 * @property languageCode Locale code for the translated text.
 * @property name Localized display name.
 * @property stateId Parent state or province id.
 */
data class NewStateTranslationData(
    val languageCode: String,
    val name: String,
    val stateId: Long
)
