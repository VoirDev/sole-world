package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.StateTranslationsTable
import dev.voir.sole.world.api.database.table.StatesTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for state or province records.
 * @property id Primary key for the record.
 * @property name Display name, localized by mapper functions when a language is requested.
 * @property stateCode State or province code, when available.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property type Administrative division type, when available.
 * @property countryId Parent country id.
 */
data class StateData(
    val id: Long,
    val name: String,
    val stateCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
    val countryId: Int,
) {
    companion object {
        /**
         * Maps an Exposed result row to a state or province API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return State or province API model populated from this row.
         */
        fun ResultRow.toState(languageCode: String?) = StateData(
            id = this[StatesTable.id].value,
            name = if (languageCode == null)
                this[StatesTable.name]
            else
                this.getOrNull(StateTranslationsTable.name) ?: this[StatesTable.name],
            stateCode = this[StatesTable.stateCode],
            latitude = this[StatesTable.latitude],
            longitude = this[StatesTable.longitude],
            type = this[StatesTable.type],
            countryId = this[StatesTable.country].value
        )
    }
}
