package dev.voir.sole.world.api.model

/**
 * API model returned for translation locale records.
 * @property id BCP 47 language tag, such as `pt-BR`.
 * @property languageId Language the locale is a form of.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property nativeName Name written in the locale itself.
 */
data class LocaleData(
    val id: String,
    val languageId: String,
    val name: String,
    val nativeName: String,
)
