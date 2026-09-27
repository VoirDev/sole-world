package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.dataset.index.PageMetadata
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.locale.RequestLocale
import dev.voir.sole.world.openapi.model.PageInfo
import jakarta.servlet.http.HttpServletRequest

/** Turns REST request parameters into the normalized values the stores expect. */
object RestRequest {
    private const val MAX_SEARCH_QUERY_LENGTH = 100

    /**
     * Reads the translation language negotiated for a request.
     *
     * The `lang` parameter and `Accept-Language` are negotiated by `RequestLocaleFilter` before the
     * handler runs, so a controller's own `lang` argument needs no further attention.
     *
     * @param request Current HTTP request.
     * @return Supported locale id, or null to serve base data.
     */
    fun language(request: HttpServletRequest): String? = RequestLocale.of(request)

    /**
     * Validates pagination parameters.
     * @param page Zero-based page number.
     * @param size Records per page.
     * @return Normalized page request.
     */
    fun pageRequest(page: Int, size: Int): PageRequest = Pagination.request(page, size)

    /**
     * Prepares an optional search or filter query.
     * @param query Raw query parameter.
     * @return Prepared query, or null when the caller supplied no meaningful value.
     * @throws IllegalArgumentException When the query is longer than the supported length.
     */
    fun searchQuery(query: String?): SearchQuery? {
        val cleanQuery = query?.trim()?.ifBlank { null } ?: return null
        require(cleanQuery.length <= MAX_SEARCH_QUERY_LENGTH) {
            "query cannot be longer than $MAX_SEARCH_QUERY_LENGTH characters"
        }

        return SearchQuery.of(cleanQuery)
    }

    /**
     * Converts internal page metadata into the published REST type.
     * @param metadata Page metadata produced by a store.
     * @return REST page information.
     */
    fun pageInfo(metadata: PageMetadata) = PageInfo(
        page = metadata.page,
        // The generator renames `size` to avoid clashing with Collection.size; the wire name is
        // still "size", so the published contract is unaffected.
        propertySize = metadata.size,
        totalItems = metadata.totalItems,
        totalPages = metadata.totalPages,
        hasNextPage = metadata.hasNextPage,
        hasPreviousPage = metadata.hasPreviousPage,
    )
}
