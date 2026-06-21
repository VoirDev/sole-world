package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized subregion name.
 * @param id Exposed entity identifier for the row.
 */
class SubregionTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SubregionTranslationEntity>(SubregionTranslationsTable)

    /** Referenced parent subregion record. */
    var subregion by SubregionEntity referencedOn SubregionTranslationsTable.subregion

    /** BCP 47-style language code for the localized text. */
    var languageCode by SubregionTranslationsTable.languageCode

    /** Primary display name. */
    var name by SubregionTranslationsTable.name
}
