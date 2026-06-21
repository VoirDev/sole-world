package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized timezone labels. */
object TimezoneTranslationsTable : LongIdTable("timezone_translations") {
    /** Referenced timezone record. */
    val timezone = reference("timezone_id", TimezonesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Display timezone name. */
    val tzName = varchar("tz_name", 100)
}
