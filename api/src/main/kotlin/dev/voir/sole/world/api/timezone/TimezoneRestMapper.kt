package dev.voir.sole.world.api.timezone

import dev.voir.sole.world.api.model.TimezoneData
import dev.voir.sole.world.openapi.model.Timezone

/**
 * Maps a timezone onto its published REST type.
 * @return REST timezone.
 */
fun TimezoneData.toRest() = Timezone(
    id = id,
    zoneName = zoneName,
    tzName = tzName,
    gmtOffset = gmtOffset,
    gmtOffsetName = gmtOffsetName,
    abbreviation = abbreviation,
)
