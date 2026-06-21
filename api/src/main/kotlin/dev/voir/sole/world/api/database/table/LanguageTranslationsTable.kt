package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized language names and descriptions. */
object LanguageTranslationsTable : LongIdTable("language_translations") {
    /** Referenced language record. */
    val language = reference("language_id", LanguagesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 100)

    /** Human-readable description. */
    val description = text("description").nullable()
}
