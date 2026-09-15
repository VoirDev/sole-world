package dev.voir.sole.world.api.subregion

import dev.voir.sole.world.api.model.SubregionData
import dev.voir.sole.world.graphql.dto.types.Subregion

/**
 * Maps a subregion onto its generated GraphQL type.
 * @return GraphQL subregion type.
 */
fun SubregionData.toGql() = Subregion(
    id = id.toString(),
    name = name,
    regionId = regionId.toString(),
)
