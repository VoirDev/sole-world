package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized city names. */
object CityTranslationsTable : LongIdTable("city_translations") {
    /** Referenced city record. */
    val city = reference("city_id", CitiesTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 255)
}
