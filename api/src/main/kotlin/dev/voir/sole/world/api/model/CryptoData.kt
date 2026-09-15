package dev.voir.sole.world.api.model

import kotlinx.datetime.LocalDate

/**
 * API model returned for cryptocurrency records.
 * @property id Primary key for the record.
 * @property code Ticker symbol the coin trades under.
 * @property alias Stable lowercase key for the coin.
 * @property name Display name.
 * @property description Description text, when available.
 * @property websiteUrl Official project website, when known.
 * @property introducedYear Year the coin launched, when known.
 * @property decimalDigits Number of decimal digits used for minor units.
 * @property obsolete Whether the coin is no longer active.
 * @property obsoleteAt Date the coin became obsolete, when known.
 * @property logoId Media asset id of the coin's logo, when one is bundled.
 */
data class CryptoData(
    val id: Long,
    val code: String,
    val alias: String,
    val name: String,
    val description: String?,
    val websiteUrl: String?,
    val introducedYear: Int?,
    val decimalDigits: Int,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val logoId: Long?,
)
