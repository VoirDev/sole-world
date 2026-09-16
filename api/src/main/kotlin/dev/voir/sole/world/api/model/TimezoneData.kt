package dev.voir.sole.world.api.model

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
)
