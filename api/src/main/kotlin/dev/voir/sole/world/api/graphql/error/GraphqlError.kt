package dev.voir.sole.world.api.graphql.error

import kotlinx.serialization.Serializable

@Serializable
data class GraphqlError(
    val message: String,
    val extensions: GraphqlErrorExtensions,
)
