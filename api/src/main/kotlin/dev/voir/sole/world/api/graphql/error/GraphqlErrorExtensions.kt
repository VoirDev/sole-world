package dev.voir.sole.world.api.graphql.error

import kotlinx.serialization.Serializable

@Serializable
data class GraphqlErrorExtensions(
    val code: String,
)
