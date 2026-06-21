package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity for a timezone and its countries and translations.
 * @param id Exposed entity identifier for the row.
 */
class TimezoneEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TimezoneEntity>(TimezonesTable)

    /** IANA timezone identifier. */
    var zoneName by TimezonesTable.zoneName

    /** Display timezone name. */
    var tzName by TimezonesTable.tzName

    /** Short timezone abbreviation. */
    var abbreviation by TimezonesTable.abbreviation

    /** GMT offset in seconds. */
    var gmtOffset by TimezonesTable.gmtOffset

    /** Formatted GMT offset label. */
    var gmtOffsetName by TimezonesTable.gmtOffsetName

    /** Localized rows associated with this record. */
    val translations by TimezoneTranslationEntity referrersOn TimezoneTranslationsTable.timezone

    /** Countries associated through a join table. */
    val countries by CountryEntity via CountryTimezonesTable
}
