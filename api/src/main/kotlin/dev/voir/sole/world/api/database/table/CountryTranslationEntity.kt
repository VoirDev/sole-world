package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity containing a localized country name.
 * @param id Exposed entity identifier for the row.
 */
class CountryTranslationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CountryTranslationEntity>(CountryTranslationsTable)

    /** Referenced country record. */
    var country by CountryEntity referencedOn CountryTranslationsTable.country

    /** BCP 47-style language code for the localized text. */
    var languageCode by CountryTranslationsTable.languageCode

    /** Primary display name. */
    var name by CountryTranslationsTable.name
}
