package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.Table

/** Join table linking countries to currencies used there. */
object CountryCurrenciesTable : Table("country_currencies") {
    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /** Referenced currency record. */
    val currency = reference("currency_id", CurrenciesTable)

    /** Composite primary key for the join table. */
    override val primaryKey = PrimaryKey(country, currency)
}
