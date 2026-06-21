package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity for a city, including coordinates and localized names.
 * @param id Exposed entity identifier for the row.
 */
class CityEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CityEntity>(CitiesTable)

    /** Referenced state or province record. */
    var state by StateEntity referencedOn CitiesTable.state

    /** Raw state foreign key value. */
    var stateId by CitiesTable.state

    /** Primary display name. */
    var name by CitiesTable.name

    /** Latitude coordinate in decimal degrees. */
    var latitude by CitiesTable.latitude

    /** Longitude coordinate in decimal degrees. */
    var longitude by CitiesTable.longitude

    /** Localized rows associated with this record. */
    val translations by CityTranslationEntity referrersOn CityTranslationsTable.city
}
