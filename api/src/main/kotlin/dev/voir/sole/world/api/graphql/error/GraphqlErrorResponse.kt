package dev.voir.sole.world.api.graphql.error

import kotlinx.serialization.Serializable

@Serializable
data class GraphqlErrorResponse(
    val errors: List<GraphqlError>,
) {
    companion object {
        val Unauthorized = GraphqlErrorResponse(
            errors = listOf(
                GraphqlError(
                    message = "Unauthorized",
                    extensions = GraphqlErrorExtensions(code = "UNAUTHORIZED"),
                )
            )
        )

    }
}
