package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.CitiesTable
import dev.voir.sole.world.api.database.table.CityTranslationsTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for city records.
 * @property id Primary key for the record.
 * @property stateId Parent state or province id.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 */
data class CityData(
    val id: Long,
    val stateId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        /**
         * Maps an Exposed result row to a city API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return City API model populated from this row.
         */
        fun ResultRow.toCity(languageCode: String?) = CityData(
            id = this[CitiesTable.id].value,
            name = if (languageCode == null)
                this[CitiesTable.name]
            else
                this.getOrNull(CityTranslationsTable.name) ?: this[CitiesTable.name],
            stateId = this[CitiesTable.state].value,
            latitude = this[CitiesTable.latitude],
            longitude = this[CitiesTable.longitude]
        )
    }
}
