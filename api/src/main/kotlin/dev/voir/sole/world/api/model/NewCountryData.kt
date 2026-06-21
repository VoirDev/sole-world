package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a country row.
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
data class NewCountryData(
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
)
