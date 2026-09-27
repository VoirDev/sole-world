package dev.voir.sole.world.api.timezone

import dev.voir.sole.world.api.model.TimezoneData
import dev.voir.sole.world.graphql.dto.types.Timezone

/**
 * Maps a timezone onto its generated GraphQL type.
 * @return GraphQL timezone type.
 */
fun TimezoneData.toGql() = Timezone(
    id = id,
    zoneName = zoneName,
    tzName = tzName,
    gmtOffset = gmtOffset,
    gmtOffsetName = gmtOffsetName,
    abbreviation = abbreviation,
)
