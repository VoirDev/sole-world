package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a currency and its lifecycle, display, country, and translation relationships.
 * @param id Exposed entity identifier for the row.
 */
class CurrencyEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CurrencyEntity>(CurrenciesTable)

    /** ISO 3166-1 alpha-3 country code or ISO 4217 alpha code. */
    var iso3 by CurrenciesTable.iso3

    /** Numeric ISO code represented as text to preserve leading zeros. */
    var isoNumeric by CurrenciesTable.isoNumeric

    /** Primary display name. */
    var name by CurrenciesTable.name

    /** Human-readable description. */
    var description by CurrenciesTable.description

    /** Name in the native language or script, when available. */
    var nativeName by CurrenciesTable.nativeName

    /** Currency symbol, when available. */
    var symbol by CurrenciesTable.symbol

    /** Introduction year, when known. */
    var year by CurrenciesTable.year

    /** Date when the currency was introduced, when known. */
    var introducedDate by CurrenciesTable.introducedDate

    /** Whether the currency is no longer active. */
    var obsolete by CurrenciesTable.obsolete

    /** Date when the currency became obsolete, when known. */
    var obsoleteAt by CurrenciesTable.obsoleteAt

    /** Replacement currency reference for obsolete currencies. */
    var replacedBy by CurrenciesTable.replacedBy

    /** Raw shared flag foreign key value. */
    var flagId by CurrenciesTable.flag

    /** Number of decimal digits used for currency minor units. */
    var decimalDigits by CurrenciesTable.decimalDigits

    /** Localized rows associated with this record. */
    val translations by CurrenciesTranslationEntity referrersOn CurrenciesTranslationsTable.currency

    /** Countries associated through a join table. */
    val countries by CountryEntity via CountryCurrenciesTable
}
