package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized country names. */
object CountryTranslationsTable : LongIdTable("country_translations") {
    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)
}
