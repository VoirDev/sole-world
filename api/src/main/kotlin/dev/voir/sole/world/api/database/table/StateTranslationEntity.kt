package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized state or province name.
 * @param id Exposed entity identifier for the row.
 */
class StateTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<StateTranslationEntity>(StateTranslationsTable)

    /** Referenced state or province record. */
    var state by StateEntity referencedOn StateTranslationsTable.state

    /** BCP 47-style language code for the localized text. */
    var languageCode by StateTranslationsTable.languageCode

    /** Primary display name. */
    var name by StateTranslationsTable.name
}
