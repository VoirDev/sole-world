package dev.voir.sole.world.api.model

/**
 * API model returned for language records.
 * @property id ISO 639-1 code, such as `fr`.
 * @property code Language code.
 * @property nativeName Name in the native language or script, when available.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property description Description text, localized by mapper functions when available.
 * @property flagId Shared flag id, when available.
 */
data class LanguageData(
    val id: String,
    val code: String,
    val nativeName: String?,
    val name: String,
    val description: String?,
    val flagId: String?,
)
