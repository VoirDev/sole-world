package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Language record read from the bundled seed data.
 * @property id Language primary key used during import.
 * @property code Language code.
 * @property nativeName Native-language name, when available.
 * @property name Default language name.
 * @property flagId Shared flag id, when available.
 * @property translations Localized language names.
 */
@Serializable
data class LanguageJSON(
    val id: Long,
    val code: String,
    val nativeName: String?,
    val name: String,
    val description: String? = null,
    val flagId: Long?,
    val translations: List<LanguageTranslationJSON>,
)
