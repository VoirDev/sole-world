package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.CentralBankTranslationsTable
import dev.voir.sole.world.api.database.table.CentralBanksTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for central bank records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property nativeName Name in the native language or script, when available.
 * @property websiteURL Official website URL, when available.
 * @property establishmentYear Year the central bank was established, when known.
 */
data class CentralBankData(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val websiteURL: String?,
    val establishmentYear: Int?
) {
    companion object {
        /**
         * Maps an Exposed result row to a central bank API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Central bank API model populated from this row.
         */
        fun ResultRow.toCentralBank(languageCode: String?) = CentralBankData(
            id = this[CentralBanksTable.id].value,
            name = if (languageCode == null)
                this[CentralBanksTable.name]
            else
                this.getOrNull(CentralBankTranslationsTable.name) ?: this[CentralBanksTable.name],
            nativeName = this[CentralBanksTable.nativeName],
            websiteURL = this[CentralBanksTable.websiteURL],
            establishmentYear = this[CentralBanksTable.establishmentYear]
        )
    }
}
