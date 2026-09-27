package dev.voir.sole.world.api.model

/**
 * API model returned for timezone records.
 * @property id IANA timezone name, such as `Europe/Paris`.
 * @property zoneName IANA timezone identifier.
 * @property tzName Timezone display name, localized by mapper functions when available.
 * @property abbreviation Timezone abbreviation.
 * @property gmtOffset GMT offset in seconds.
 * @property gmtOffsetName Formatted GMT offset label.
 */
data class TimezoneData(
    val id: String,
    val zoneName: String,
    val tzName: String,
    val abbreviation: String,
    val gmtOffset: Int,
    val gmtOffsetName: String,
)
