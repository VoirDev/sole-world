package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Region record read from bundled region seed data.
 * @property id Region alias, such as `europe`.
 * @property name Default region name.
 * @property wikiDataId Wikidata identifier for the region.
 * @property translations Localized region names.
 * @property subregions Subregions nested under this region.
 */
@Serializable
data class RegionJSON(
    val id: String,
    val name: String,
    val wikiDataId: String,
    val translations: List<RegionTranslationJSON>,
    val subregions: List<SubregionJSON>,
)
