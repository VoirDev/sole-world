package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.Table


/** Join table linking central banks to the countries they serve. */
object CentralBankCountriesTable : Table("central_bank_countries") {
    /** Referenced central bank record. */
    val centralBank = reference("central_bank_id", CentralBanksTable)

    /** Referenced country record. */
    val country = reference("country_id", CountriesTable)

    /** Composite primary key for the join table. */
    override val primaryKey = PrimaryKey(centralBank, country)
}
