package dev.voir.sole.world.api.model

/**
 * Seed/import payload used to create a central bank row.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property nativeName Name in the native language or script, when available.
 * @property websiteURL Official website URL, when available.
 * @property establishmentYear Year the central bank was established, when known.
 */
data class NewCentralBankData(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val websiteURL: String?,
    val establishmentYear: Int?
)
