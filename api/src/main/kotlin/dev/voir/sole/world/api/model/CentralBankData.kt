package dev.voir.sole.world.api.model

/**
 * API model returned for central bank records.
 * @property id Central bank alias, such as `federal-reserve`.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property nativeName Name in the native language or script, when available.
 * @property websiteURL Official website URL, when available.
 * @property establishmentYear Year the central bank was established, when known.
 */
data class CentralBankData(
    val id: String,
    val name: String,
    val nativeName: String?,
    val websiteURL: String?,
    val establishmentYear: Int?,
)
