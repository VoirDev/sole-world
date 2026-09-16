package dev.voir.sole.world.api.model

import kotlinx.datetime.LocalDate

/**
 * API model returned for currency records.
 * @property id Primary key for the record.
 * @property iso3 ISO alpha code for the record.
 * @property isoNumeric Numeric ISO code represented as text to preserve leading zeros.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property decimalDigits Number of decimal digits used for minor currency units.
 * @property popularity How widely the currency is used today, from 0 to 100.
 * @property description Description text, localized by mapper functions when available.
 * @property nativeName Name in the native language or script, when available.
 * @property symbol Currency symbol, when available.
 * @property year Introduction year, when known.
 * @property introducedDate Date when the currency was introduced, when known.
 * @property obsolete Whether the currency is no longer active.
 * @property obsoleteAt Date when the currency became obsolete, when known.
 * @property replacedById Replacement currency id for obsolete currencies, when available.
 * @property flagId Shared flag id, when available.
 */
data class CurrencyData(
    val id: Long,
    val iso3: String,
    val isoNumeric: String,
    val name: String,
    val decimalDigits: Int,
    val popularity: Int,
    val description: String?,
    val nativeName: String?,
    val symbol: String?,
    val year: Int?,
    val introducedDate: LocalDate?,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val replacedById: Long?,
    val flagId: Long?,
)
