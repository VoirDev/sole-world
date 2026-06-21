package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** Stores localized central bank names. */
object CentralBankTranslationsTable : LongIdTable("central_bank_translations") {
    /** Referenced central bank record. */
    val centralBank = reference("central_bank_id", CentralBanksTable)

    /** BCP 47-style language code for the localized text. */
    val languageCode = varchar("language_code", 10)

    /** Primary display name. */
    val name = varchar("name", 100)
}
