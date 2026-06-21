package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.SubregionTranslationsTable
import dev.voir.sole.world.api.database.table.SubregionsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for subregion records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property regionId Parent region id.
 */
data class SubregionData(
    val id: Int,
    val name: String,
    val regionId: Int,
) {
    companion object {
        /**
         * Maps an Exposed result row to a subregion API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Subregion API model populated from this row.
         */
        fun ResultRow.toSubregion(languageCode: String?) = SubregionData(
            id = this[SubregionsTable.id].value,
            name = if (languageCode == null)
                this[SubregionsTable.name]
            else
                this.getOrNull(SubregionTranslationsTable.name) ?: this[SubregionsTable.name],
            regionId = this[SubregionsTable.region].value
        )
    }
}
