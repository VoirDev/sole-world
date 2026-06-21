package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.Table

/** Join table linking countries to their timezones. */
object CountryTimezonesTable : Table("country_timezones") {
    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /** Referenced timezone record. */
    val timezone = reference("timezone_id", TimezonesTable)

    /** Composite primary key for the join table. */
    override val primaryKey = PrimaryKey(country, timezone)
}
