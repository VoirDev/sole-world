package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Country record read from an individual bundled country seed file.
 * @property id ISO 3166-1 alpha-2 code, such as `US`.
 * @property name Default country name.
 * @property nativeName Native-language country name, when available.
 * @property iso2 ISO 3166-1 alpha-2 country code.
 * @property iso3 ISO 3166-1 alpha-3 country code.
 * @property numericCode ISO 3166-1 numeric country code.
 * @property phoneCode International calling code.
 * @property tld Country top-level domain, when available.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property flagId Shared flag id, when available.
 * @property regionId Parent region id.
 * @property subregionId Parent subregion id.
 * @property currencyIds Currency ids associated with the country.
 * @property timezoneIds Timezone ids associated with the country.
 * @property officialLanguageIds Official language ids associated with the country.
 * @property otherLanguageIds Other language ids associated with the country.
 * @property translations Localized country names.
 * @property states States or provinces nested under the country.
 */
@Serializable
data class CountryJSON(
    val id: String,
    val name: String,
    val nativeName: String?,
    val iso2: String,
    val iso3: String,
    val numericCode: String,
    val phoneCode: String,
    val tld: String?,
    val latitude: Double,
    val longitude: Double,
    val flagId: String?,
    val regionId: String,
    val subregionId: String,
    val currencyIds: List<String>,
    val timezoneIds: List<String>,
    val officialLanguageIds: List<String>,
    val otherLanguageIds: List<String>,
    val translations: List<CountryTranslationJSON>,
    val states: List<StateJSON>,
)
