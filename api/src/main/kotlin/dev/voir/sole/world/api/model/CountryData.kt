package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.CountriesTable
import dev.voir.sole.world.api.database.table.CountryTranslationsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for country records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property nativeName Name in the native language or script, when available.
 * @property iso3 ISO alpha code for the record.
 * @property iso2 ISO 3166-1 alpha-2 country code.
 * @property isoNumeric Numeric ISO code represented as text to preserve leading zeros.
 * @property phoneCode International calling code.
 * @property tld Country top-level domain, when available.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property flagId Shared flag id, when available.
 * @property regionId Parent region id.
 * @property subregionId Parent subregion id.
 */
data class CountryData(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val iso3: String,
    val iso2: String,
    val isoNumeric: String,
    val phoneCode: String,
    val tld: String?,
    val latitude: Double,
    val longitude: Double,
    val flagId: Int?,
    val regionId: Int,
    val subregionId: Int,
) {
    companion object {
        /**
         * Maps an Exposed result row to a country API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Country API model populated from this row.
         */
        fun ResultRow.toCountry(languageCode: String?) = CountryData(
            id = this[CountriesTable.id].value,
            name = if (languageCode == null)
                this[CountriesTable.name]
            else
                this.getOrNull(CountryTranslationsTable.name) ?: this[CountriesTable.name],
            nativeName = this[CountriesTable.nativeName],
            iso3 = this[CountriesTable.iso3],
            iso2 = this[CountriesTable.iso2],
            isoNumeric = this[CountriesTable.isoNumeric],
            phoneCode = this[CountriesTable.phoneCode],
            tld = this[CountriesTable.tld],
            latitude = this[CountriesTable.latitude],
            longitude = this[CountriesTable.longitude],
            flagId = this[CountriesTable.flag]?.value,
            regionId = this[CountriesTable.region].value,
            subregionId = this[CountriesTable.subregion].value,
        )
    }
}
