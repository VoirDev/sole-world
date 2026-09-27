package dev.voir.sole.world.api.region

import dev.voir.sole.world.api.model.RegionData
import dev.voir.sole.world.graphql.dto.types.Region

/**
 * Maps a region onto its generated GraphQL type.
 * @return GraphQL region type.
 */
fun RegionData.toGql() = Region(
    id = id,
    name = name,
)
