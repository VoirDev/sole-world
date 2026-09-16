package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Cryptocurrency record read from the bundled seed data.
 * @property id Cryptocurrency primary key used during import.
 * @property code Ticker symbol the coin trades under.
 * @property alias Stable lowercase key, also the name its logo files are stored under.
 * @property name Display name.
 * @property description Description text, when available.
 * @property websiteUrl Official project website, when known.
 * @property introducedYear Year the coin launched.
 * @property decimalDigits Number of decimal digits used for minor units.
 * @property obsolete Whether the coin is no longer active.
 * @property obsoleteAt Date string in YYYY-MM-DD format, when the coin became obsolete.
 * @property logoId Media asset id of the coin's logo, when one is bundled.
 */
@Serializable
data class CryptoJSON(
    val id: Long,
    val code: String,
    val alias: String,
    val name: String,
    val description: String?,
    val websiteUrl: String?,
    val introducedYear: Int?,
    val decimalDigits: Int,
    val obsolete: Boolean,
    val obsoleteAt: String?,
    val logoId: Long?,
)
