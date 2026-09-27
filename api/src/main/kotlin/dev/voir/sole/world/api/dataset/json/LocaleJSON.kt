package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Translation locale read from the bundled seed data: a language the API can answer in.
 * @property id BCP 47 language tag, such as `pt-BR`.
 * @property languageId Language the locale is a form of.
 * @property name Default locale name.
 * @property nativeName Locale name written in the locale itself.
 * @property translations Localized locale names.
 */
@Serializable
data class LocaleJSON(
    val id: String,
    val languageId: String,
    val name: String,
    val nativeName: String,
    val translations: List<LocaleTranslationJSON>,
)
