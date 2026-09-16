package dev.voir.sole.world.api.state

import dev.voir.sole.world.api.graphql.EmptyPage
import dev.voir.sole.world.api.model.StateData
import dev.voir.sole.world.graphql.dto.types.Coordinates
import dev.voir.sole.world.graphql.dto.types.State

/**
 * Maps a state onto its generated GraphQL type.
 * @return GraphQL state type.
 */
fun StateData.toGql() = State(
    id = id.toString(),
    name = name,
    stateCode = stateCode,
    coordinates = if (latitude == null || longitude == null) {
        null
    } else {
        Coordinates(latitude = latitude, longitude = longitude)
    },
    type = type,
    countryId = countryId.toString(),
    cities = EmptyPage.CITIES,
)
