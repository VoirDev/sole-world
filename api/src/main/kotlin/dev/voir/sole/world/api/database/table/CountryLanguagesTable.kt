package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.Table

/** Join table linking countries to languages used there. */
object CountryLanguagesTable : Table("country_languages") {
    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /** Referenced language record. */
    val language = reference("language_id", LanguagesTable)

    /** Classification value for this record. */
    val type = varchar("type", 10) // Official, other

    /** Composite primary key for the join table. */
    override val primaryKey = PrimaryKey(country, language)
}
