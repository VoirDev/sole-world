package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a language row.
 * @property id Primary key for the record.
 * @property code Language code.
 * @property nativeName Name in the native language or script, when available.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property description Description text, localized by mapper functions when available.
 * @property flagId Shared flag id, when available.
 */
data class NewLanguageData(
    val id: Int,
    val code: String,
    val nativeName: String?,
    val name: String,
    val description: String?,
    val flagId: Int?,
)
