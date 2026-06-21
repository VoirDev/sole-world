package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Region record read from bundled region seed data.
 * @property id Region primary key used during import.
 * @property name Default region name.
 * @property wikiDataId Wikidata identifier for the region.
 * @property translations Localized region names.
 * @property subregions Subregions nested under this region.
 */
@Serializable
internal data class RegionJSON(
    val id: Int,
    val name: String,
    val wikiDataId: String,
    val translations: List<RegionTranslationJSON>,
    val subregions: List<SubregionJSON>
)
