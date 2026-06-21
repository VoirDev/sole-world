package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.LanguageTranslationsTable
import dev.voir.sole.world.api.database.table.LanguagesTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for language records.
 * @property id Primary key for the record.
 * @property code Language code.
 * @property nativeName Name in the native language or script, when available.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property description Description text, localized by mapper functions when available.
 * @property flagId Shared flag id, when available.
 */
data class LanguageData(
    val id: Int,
    val code: String,
    val nativeName: String?,
    val name: String,
    val description: String?,
    val flagId: Int?,
) {
    companion object {
        /**
         * Maps an Exposed result row to a language API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Language API model populated from this row.
         */
        fun ResultRow.toLanguage(languageCode: String?) = LanguageData(
            id = this[LanguagesTable.id].value,
            code = this[LanguagesTable.code],
            nativeName = this[LanguagesTable.nativeName],
            name = if (languageCode == null)
                this[LanguagesTable.name]
            else
                this.getOrNull(LanguageTranslationsTable.name) ?: this[LanguagesTable.name],
            description = if (languageCode == null)
                this[LanguagesTable.description]
            else
                this.getOrNull(LanguageTranslationsTable.description)
                    ?: this[LanguagesTable.description],
            flagId = this[LanguagesTable.flag]?.value,
        )
    }
}
