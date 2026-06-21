package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized city name.
 * @param id Exposed entity identifier for the row.
 */
class CityTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CityTranslationEntity>(CityTranslationsTable)

    /** Referenced city record. */
    var city by CityEntity referencedOn CityTranslationsTable.city

    /** BCP 47-style language code for the localized text. */
    var languageCode by CityTranslationsTable.languageCode

    /** Primary display name. */
    var name by CityTranslationsTable.name
}
