package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Subregion record nested under a region seed record.
 * @property id Subregion alias, such as `western-europe`.
 * @property name Default subregion name.
 * @property wikiDataId Wikidata identifier for the subregion.
 * @property translations Localized subregion names.
 */
@Serializable
data class SubregionJSON(
    val id: String,
    val name: String,
    val wikiDataId: String,
    val translations: List<SubregionTranslationJSON>,
)
