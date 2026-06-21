package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing localized timezone labels.
 * @param id Exposed entity identifier for the row.
 */
class TimezoneTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TimezoneTranslationEntity>(TimezoneTranslationsTable)

    /** Referenced timezone record. */
    var timezone by TimezoneEntity referencedOn TimezoneTranslationsTable.timezone

    /** BCP 47-style language code for the localized text. */
    var languageCode by TimezoneTranslationsTable.languageCode

    /** Display timezone name. */
    var tzName by TimezoneTranslationsTable.tzName
}
