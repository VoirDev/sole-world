package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a world subregion and its countries and translations.
 * @param id Exposed entity identifier for the row.
 */
class SubregionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SubregionEntity>(SubregionsTable)

    /** Primary display name. */
    var name by SubregionsTable.name

    /** Referenced parent region record. */
    var region by RegionEntity referencedOn SubregionsTable.region

    /** Raw region foreign key value. */
    var regionId by SubregionsTable.region

    /** Wikidata identifier for external lookup. */
    var wikiDataId by SubregionsTable.wikiDataId

    /** Localized rows associated with this record. */
    val translations by SubregionTranslationEntity referrersOn SubregionTranslationsTable.subregion

    /** Countries associated through a join table. */
    val countries by CountryEntity referrersOn CountriesTable.subregion
}
