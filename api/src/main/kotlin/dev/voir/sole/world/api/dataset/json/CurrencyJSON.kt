package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Currency record read from the bundled seed data.
 * @property id Currency primary key used during import.
 * @property iso3 ISO 4217 alpha code.
 * @property isoNumeric ISO 4217 numeric code.
 * @property name Default currency name.
 * @property description Default currency description, when available.
 * @property nativeName Native-language currency name, when available.
 * @property year Introduction year, when known.
 * @property introducedDate Introduction date string in YYYY-MM-DD format, when available.
 * @property obsolete Whether the currency is obsolete.
 * @property obsoleteAt Obsolete date string, when available.
 * @property replacedBy Replacement currency id, when available.
 * @property symbol Currency symbol, when available.
 * @property flagId Shared flag id, when available.
 * @property translations Localized currency names.
 * @property decimalDigits Number of decimal digits used for minor units.
 * @property popularity How widely the currency is used, from 0 to 100.
 */
@Serializable
data class CurrencyJSON(
    val id: Long,
    val iso3: String,
    val isoNumeric: String,
    val name: String,
    val description: String?,
    val nativeName: String?,
    val year: Int?,
    val introducedDate: String?, // YYYY-MM-DD
    val obsolete: Boolean,
    val obsoleteAt: String?,
    val replacedBy: Long?,
    val symbol: String?,
    val flagId: Long?,
    val translations: List<CurrencyTranslationJSON>,
    val decimalDigits: Int,
    val popularity: Int = 0,
)
