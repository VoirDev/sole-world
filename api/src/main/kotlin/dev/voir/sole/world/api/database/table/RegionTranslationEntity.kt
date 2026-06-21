package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized region name.
 * @param id Exposed entity identifier for the row.
 */
class RegionTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RegionTranslationEntity>(RegionTranslationsTable)

    /** Referenced parent region record. */
    var region by RegionEntity referencedOn RegionTranslationsTable.region

    /** BCP 47-style language code for the localized text. */
    var languageCode by RegionTranslationsTable.languageCode

    /** Primary display name. */
    var name by RegionTranslationsTable.name
}
