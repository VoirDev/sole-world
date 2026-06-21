package dev.voir.sole.world.api.service

import kotlinx.serialization.Serializable

/**
 * Subregion record nested under a region seed record.
 * @property id Subregion primary key used during import.
 * @property name Default subregion name.
 * @property wikiDataId Wikidata identifier for the subregion.
 * @property translations Localized subregion names.
 */
@Serializable
internal data class SubregionJSON(
    val id: Int,
    val name: String,
    val wikiDataId: String,
    val translations: List<SubregionTranslationJSON>
)
