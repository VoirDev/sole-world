package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.Table

/** Join table linking central banks to the currencies they issue. */
object CentralBankIssuedCurrenciesTable : Table("central_bank_issued_currencies") {
    /** Referenced central bank record. */
    val centralBank = reference("central_bank_id", CentralBanksTable)

    /** Referenced currency record. */
    val currency = reference("currency_id", CurrenciesTable)

    /** Composite primary key for the join table. */
    override val primaryKey = PrimaryKey(centralBank, currency)
}
