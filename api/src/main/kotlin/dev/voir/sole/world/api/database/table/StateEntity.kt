package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * DAO entity for a first-level administrative division and its cities and translations.
 * @param id Exposed entity identifier for the row.
 */
class StateEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<StateEntity>(StatesTable)

    /** Primary display name. */
    var name by StatesTable.name

    /** State or province code, when available. */
    var stateCode by StatesTable.stateCode

    /** Latitude coordinate in decimal degrees. */
    var latitude by StatesTable.latitude

    /** Longitude coordinate in decimal degrees. */
    var longitude by StatesTable.longitude

    /** Classification value for this record. */
    var type by StatesTable.type

    /** Referenced country record. */
    var country by CountryEntity referencedOn StatesTable.country

    /** Raw country foreign key value. */
    var countryId by StatesTable.country

    /** Cities belonging to this record. */
    val cities by CityEntity referrersOn CitiesTable.state

    /** Localized rows associated with this record. */
    val translations by StateTranslationEntity referrersOn StateTranslationsTable.state
}
