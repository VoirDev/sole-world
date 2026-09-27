package dev.voir.sole.world.api.dataset.json

import kotlinx.serialization.Serializable

/**
 * Timezone record read from bundled seed data.
 * @property id IANA timezone name, such as `Europe/Paris`.
 * @property zoneName IANA timezone identifier.
 * @property gmtOffset GMT offset in seconds.
 * @property gmtOffsetName Formatted GMT offset label.
 * @property abbreviation Timezone abbreviation.
 * @property tzName Default timezone display name.
 * @property exemplarCity Human-readable representative city or locality from the IANA identifier.
 * @property translations Localized timezone labels.
 */
@Serializable
data class TimezoneJSON(
    val id: String,
    val zoneName: String,
    val gmtOffset: Int,
    val gmtOffsetName: String,
    val abbreviation: String,
    val tzName: String,
    val exemplarCity: String? = null,
    val translations: List<TimezoneTranslationJSON>,
)
