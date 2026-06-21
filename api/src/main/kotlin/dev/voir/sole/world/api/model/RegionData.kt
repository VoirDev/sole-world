package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.RegionTranslationsTable
import dev.voir.sole.world.api.database.table.RegionsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for region records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 */
data class RegionData(
    val id: Int,
    val name: String,
) {
    companion object {
        /**
         * Maps an Exposed result row to a region API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Region API model populated from this row.
         */
        fun ResultRow.toRegion(languageCode: String?) = RegionData(
            id = this[RegionsTable.id].value,
            name = if (languageCode == null)
                this[RegionsTable.name]
            else
                this.getOrNull(RegionTranslationsTable.name) ?: this[RegionsTable.name],
        )
    }
}
