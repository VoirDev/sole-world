package dev.voir.sole.world.api.graphql

import dev.voir.sole.world.graphql.dto.types.CityPage
import dev.voir.sole.world.graphql.dto.types.PageInfo
import dev.voir.sole.world.graphql.dto.types.StatePage

/**
 * Placeholders for the paginated relationship fields on generated types.
 *
 * A relationship that takes page arguments is resolved by its own field fetcher, which needs those
 * arguments and so cannot run while the parent record is being mapped. The generated type declares
 * the field non-null all the same, so the parent carries one of these and the field fetcher always
 * replaces it before anything is serialized.
 */
object EmptyPage {
    private val METADATA = PageInfo(
        page = 0,
        size = 0,
        totalItems = 0,
        totalPages = 0,
        hasNextPage = false,
        hasPreviousPage = false,
    )

    /** Stands in for a states relationship until its field fetcher resolves it. */
    val STATES = StatePage(items = emptyList(), pageInfo = METADATA)

    /** Stands in for a cities relationship until its field fetcher resolves it. */
    val CITIES = CityPage(items = emptyList(), pageInfo = METADATA)
}
