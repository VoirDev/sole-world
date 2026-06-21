package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.CurrenciesTable
import dev.voir.sole.world.api.database.table.CurrenciesTranslationsTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for currency records.
 * @property id Primary key for the record.
 * @property iso3 ISO alpha code for the record.
 * @property isoNumeric Numeric ISO code represented as text to preserve leading zeros.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property decimalDigits Number of decimal digits used for minor currency units.
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
    val id: Int,
    val iso3: String,
    val isoNumeric: String,
    val name: String,
    val decimalDigits: Int,
    val description: String?,
    val nativeName: String?,
    val symbol: String?,
    val year: Int?,
    val introducedDate: LocalDate?,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val replacedById: Int?,
    val flagId: Int?,
) {
    companion object {
        /**
         * Maps an Exposed result row to a currency API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Currency API model populated from this row.
         */
        fun ResultRow.toCurrency(languageCode: String?) = CurrencyData(
            id = this[CurrenciesTable.id].value,
            iso3 = this[CurrenciesTable.iso3],
            isoNumeric = this[CurrenciesTable.isoNumeric],
            name = if (languageCode == null)
                this[CurrenciesTable.name]
            else
                this.getOrNull(CurrenciesTranslationsTable.name) ?: this[CurrenciesTable.name],
            decimalDigits = this[CurrenciesTable.decimalDigits],
            description = if (languageCode == null) this[CurrenciesTable.description] else this[CurrenciesTranslationsTable.description]
                ?: this[CurrenciesTable.description],
            nativeName = this[CurrenciesTable.nativeName],
            symbol = this[CurrenciesTable.symbol],
            year = this[CurrenciesTable.year],
            introducedDate = this[CurrenciesTable.introducedDate],
            obsolete = this[CurrenciesTable.obsolete],
            obsoleteAt = this[CurrenciesTable.obsoleteAt],
            replacedById = this[CurrenciesTable.replacedBy]?.value,
            flagId = this[CurrenciesTable.flag]?.value,
        )
    }
}
