package dev.voir.sole.world.api.model

import kotlinx.datetime.LocalDate

/**
 * API model returned for cryptocurrency records.
 * @property id The coin's alias: its stable lowercase key, and the name its logo files are stored under.
 * @property code Ticker symbol the coin trades under.
 * @property name Display name.
 * @property description Description text, when available.
 * @property websiteUrl Official project website, when known.
 * @property introducedYear Year the coin launched, when known.
 * @property decimalDigits Number of decimal digits used for minor units.
 * @property popularity How widely the coin is held and traded today, from 0 to 100.
 * @property obsolete Whether the coin is no longer active.
 * @property obsoleteAt Date the coin became obsolete, when known.
 * @property logoId Media asset id of the coin's logo, when one is bundled.
 */
data class CryptoData(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val websiteUrl: String?,
    val introducedYear: Int?,
    val decimalDigits: Int,
    val popularity: Int,
    val obsolete: Boolean,
    val obsoleteAt: LocalDate?,
    val logoId: String?,
)
