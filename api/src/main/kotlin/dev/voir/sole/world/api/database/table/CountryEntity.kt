package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a country and its geographic, language, currency, timezone, and central bank relationships.
 * @param id Exposed entity identifier for the row.
 */
class CountryEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CountryEntity>(CountriesTable)

    /** Primary display name. */
    var name by CountriesTable.name

    /** Name in the native language or script, when available. */
    var nativeName by CountriesTable.nativeName

    /** ISO 3166-1 alpha-3 country code or ISO 4217 alpha code. */
    var iso3 by CountriesTable.iso3

    /** ISO 3166-1 alpha-2 country code. */
    var iso2 by CountriesTable.iso2

    /** Numeric ISO code represented as text to preserve leading zeros. */
    var isoNumeric by CountriesTable.isoNumeric

    /** International calling code. */
    var phoneCode by CountriesTable.phoneCode

    /** Country top-level domain, when available. */
    var tld by CountriesTable.tld

    /** Latitude coordinate in decimal degrees. */
    var latitude by CountriesTable.latitude

    /** Longitude coordinate in decimal degrees. */
    var longitude by CountriesTable.longitude

    /** Referenced parent region record. */
    var region by RegionEntity referencedOn CountriesTable.region

    /** Raw region foreign key value. */
    val regionId by CountriesTable.region

    /** Referenced parent subregion record. */
    var subregion by SubregionEntity referencedOn CountriesTable.subregion

    /** Raw subregion foreign key value. */
    val subregionId by CountriesTable.subregion

    /** Raw shared flag foreign key value. */
    var flagId by CountriesTable.flag

    /** Timezones associated through a join table. */
    var timezones by TimezoneEntity via CountryTimezonesTable

    /** Currencies associated through a join table. */
    var currencies by CurrencyEntity via CountryCurrenciesTable

    /** Languages associated through a join table. */
    var languages by LanguageEntity via CountryLanguagesTable

    /** Localized rows associated with this record. */
    val translations by CountryTranslationEntity referrersOn CountryTranslationsTable.country

    /** States or provinces belonging to this country. */
    val states by StateEntity referrersOn StatesTable.country

    /** Central banks associated with this record. */
    val centralBanks by CentralBankEntity via CentralBankCountriesTable
}
