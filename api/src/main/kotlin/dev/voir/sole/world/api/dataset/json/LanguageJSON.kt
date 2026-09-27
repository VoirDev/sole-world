package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Language record read from the bundled seed data.
 * @property id ISO 639-1 code, such as `fr`.
 * @property code Language code.
 * @property nativeName Native-language name, when available.
 * @property name Default language name.
 * @property flagId Shared flag id, when available.
 * @property translations Localized language names.
 */
@Serializable
data class LanguageJSON(
    val id: String,
    val code: String,
    val nativeName: String?,
    val name: String,
    val description: String? = null,
    val flagId: String?,
    val translations: List<LanguageTranslationJSON>,
)
