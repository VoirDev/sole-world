package dev.voir.sole.world.api.city

import dev.voir.sole.world.api.model.CityData
import dev.voir.sole.world.graphql.dto.types.City
import dev.voir.sole.world.graphql.dto.types.Coordinates

/**
 * Maps a city onto its generated GraphQL type.
 * @return GraphQL city type.
 */
fun CityData.toGql() = City(
    id = id.toString(),
    name = name,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    stateId = stateId,
)
