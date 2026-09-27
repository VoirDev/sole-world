package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Central bank record read from the bundled seed data.
 * @property id Central bank alias, such as `federal-reserve`.
 * @property name Default display name.
 * @property nativeName Native-language name, when available.
 * @property websiteURL Official website URL, when available.
 * @property establishmentYear Year the central bank was established, when known.
 * @property translations Localized central bank names.
 * @property currencyIds Currency ids issued by this central bank.
 * @property countryIds Country ids served by this central bank.
 */
@Serializable
data class CentralBankJSON(
    val id: String,
    val name: String,
    val nativeName: String?,
    val websiteURL: String?,
    val establishmentYear: Int?,
    val translations: List<CentralBankTranslationJSON>,
    val currencyIds: List<String>,
    val countryIds: List<String>,
)
