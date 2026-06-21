package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a language translation row.
 * @property languageId Parent language id.
 * @property name Localized display name.
 * @property description Description text, localized by mapper functions when available.
 */
data class NewLanguageTranslationData(
    val languageId: Int,
    val name: String,
    val description: String?,
)
