package dev.voir.sole.world.api.database.table

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

/**
 * DAO entity for a world region and its subregions, countries, and translations.
 * @param id Exposed entity identifier for the row.
 */
class RegionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RegionEntity>(RegionsTable)

    /** Primary display name. */
    var name by RegionsTable.name

    /** Wikidata identifier for external lookup. */
    var wikiDataId by RegionsTable.wikiDataId

    /** Localized rows associated with this record. */
    val translations by RegionTranslationEntity referrersOn RegionTranslationsTable.region

    /** Subregions belonging to this region. */
    val subregions by SubregionEntity referrersOn SubregionsTable.region

    /** Countries associated through a join table. */
    val countries by CountryEntity referrersOn CountriesTable.region
}
