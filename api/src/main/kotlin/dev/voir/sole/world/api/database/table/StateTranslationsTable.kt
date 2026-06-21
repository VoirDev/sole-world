package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized state or province names. */
object StateTranslationsTable : LongIdTable("state_translations") {
    /** Referenced state or province record. */
    val state = reference("state_id", StatesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)
}
