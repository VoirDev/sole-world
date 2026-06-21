package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing localized language text.
 * @param id Exposed entity identifier for the row.
 */
class LanguageTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<LanguageTranslationEntity>(LanguageTranslationsTable)

    /** Referenced language record. */
    var language by LanguageEntity referencedOn LanguageTranslationsTable.language

    /** BCP 47-style language code for the localized text. */
    var languageCode by LanguageTranslationsTable.languageCode

    /** Primary display name. */
    var name by LanguageTranslationsTable.name

    /** Human-readable description. */
    var description by LanguageTranslationsTable.description
}
