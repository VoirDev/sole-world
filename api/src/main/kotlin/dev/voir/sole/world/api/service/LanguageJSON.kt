package dev.voir.sole.world.api.service

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
internal data class LanguageJSON(
    val id: Int,
    val code: String,
    val nativeName: String?,
    val name: String,
    val flagId: Int?,
    val translations: List<LanguageTranslationJSON>,
)
