package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a central bank and its country and currency relationships.
 * @param id Exposed entity identifier for the row.
 */
class CentralBankEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CentralBankEntity>(CentralBanksTable)

    /** Primary display name. */
    var name by CentralBanksTable.name

    /** Name in the native language or script, when available. */
    var nativeName by CentralBanksTable.nativeName

    /** Official website URL, when available. */
    var websiteURL by CentralBanksTable.websiteURL

    /** Year the central bank was established, when known. */
    var establishmentYear by CentralBanksTable.establishmentYear

    /** Currencies associated through a join table. */
    var currencies by CurrencyEntity via CentralBankIssuedCurrenciesTable

    /** Countries associated through a join table. */
    var countries by CountryEntity via CentralBankCountriesTable
}
