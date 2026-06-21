package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized central bank name.
 * @param id Exposed entity identifier for the row.
 */
class CentralBankTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CentralBankTranslationEntity>(CentralBankTranslationsTable)

    /** Referenced central bank record. */
    var centralBank by CentralBankEntity referencedOn CentralBankTranslationsTable.centralBank

    /** BCP 47-style language code for the localized text. */
    var languageCode by CentralBankTranslationsTable.languageCode

    /** Primary display name. */
    var name by CentralBankTranslationsTable.name
}
