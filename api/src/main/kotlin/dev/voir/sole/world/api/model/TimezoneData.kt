package dev.voir.sole.world.api.model

import dev.voir.sole.world.api.database.table.TimezoneTranslationsTable
import dev.voir.sole.world.api.database.table.TimezonesTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * API model returned for timezone records.
 * @property id Primary key for the record.
 * @property zoneName IANA timezone identifier.
 * @property tzName Timezone display name, localized by mapper functions when available.
 * @property abbreviation Timezone abbreviation.
 * @property gmtOffset GMT offset in seconds.
 * @property gmtOffsetName Formatted GMT offset label.
 */
data class TimezoneData(
    val id: Long,
    val zoneName: String,
    val tzName: String,
    val abbreviation: String,
    val gmtOffset: Int,
    val gmtOffsetName: String,
) {
    companion object {
        /**
         * Maps an Exposed result row to a timezone API model.
         * @param languageCode Locale code used to select translated fields; null uses base table values.
         * @return Timezone API model populated from this row.
         */
        fun ResultRow.toTimezone(languageCode: String?) = TimezoneData(
            id = this[TimezonesTable.id].value,
            zoneName = this[TimezonesTable.zoneName],
            tzName = if (languageCode == null)
                this[TimezonesTable.tzName]
            else
                this.getOrNull(TimezoneTranslationsTable.tzName) ?: this[TimezonesTable.tzName],
            abbreviation = this[TimezonesTable.abbreviation],
            gmtOffset = this[TimezonesTable.gmtOffset],
            gmtOffsetName = this[TimezonesTable.gmtOffsetName]
        )
    }
}
