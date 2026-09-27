package dev.voir.sole.world.api.graphql

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import dev.voir.sole.world.api.dataset.index.PageMetadata
import dev.voir.sole.world.api.dataset.index.PageRequest
import dev.voir.sole.world.api.dataset.index.Pagination
import dev.voir.sole.world.api.dataset.index.SearchQuery
import dev.voir.sole.world.api.locale.RequestLocale
import dev.voir.sole.world.graphql.dto.types.PageInfo
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Transport-contract helpers shared by every GraphQL fetcher.
 *
 * These enforce the technical request contract — identifier shape, page bounds, query length — which
 * is the gateway's job. Nothing here decides what the data means.
 */
object GraphqlRequest {
    private const val MAX_IDS_PER_QUERY = 50
    private const val MAX_SEARCH_QUERY_LENGTH = 100

    /**
     * Reads the translation language negotiated for the current HTTP request.
     * @return Supported locale id, or null to serve base data.
     */
    fun language(): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes?.request?.let(RequestLocale::of)
    }

    /**
     * Reads the translation language a root fetcher selected for the current operation.
     *
     * Root fetchers publish the language as GraphQL local context, which graphql-java hands down to
     * every nested field. Reading it back here keeps child fetchers off the request thread-locals.
     *
     * @param dfe Data fetching environment for the nested field.
     * @return Supported internal language code, or null to serve base data.
     */
    fun language(dfe: DgsDataFetchingEnvironment): String? = dfe.getLocalContext<String>()

    /**
     * Wraps a root fetcher's payload with the language its nested fields should use.
     * @param data Payload to return to GraphQL.
     * @param languageCode Language selected for this operation.
     * @return Result carrying the payload plus local context for child fetchers.
     */
    fun <T : Any> localized(data: T?, languageCode: String?): DataFetcherResult<T> {
        return DataFetcherResult.newResult<T>()
            .data(data)
            .localContext(languageCode)
            .build()
    }

    /**
     * Validates GraphQL pagination input.
     * @param pageInput Optional pagination input.
     * @return Normalized page request.
     */
    fun pageRequest(pageInput: PageInput?): PageRequest =
        Pagination.request(page = pageInput?.page, size = pageInput?.size)

    /**
     * Converts internal page metadata into the generated GraphQL type.
     * @param metadata Page metadata produced by a store.
     * @return GraphQL page information.
     */
    fun pageInfo(metadata: PageMetadata) = PageInfo(
        page = metadata.page,
        size = metadata.size,
        totalItems = metadata.totalItems,
        totalPages = metadata.totalPages,
        hasNextPage = metadata.hasNextPage,
        hasPreviousPage = metadata.hasPreviousPage,
    )

    /**
     * Prepares an optional search or filter query.
     * @param query Raw query argument, or null when the caller supplied no filter.
     * @return Prepared query, or null when the caller supplied no meaningful filter.
     * @throws IllegalArgumentException When the query is longer than the supported length.
     */
    fun optionalSearchQuery(query: String?): SearchQuery? {
        val cleanQuery = query?.trim()?.ifBlank { null } ?: return null
        require(cleanQuery.length <= MAX_SEARCH_QUERY_LENGTH) {
            "query cannot be longer than $MAX_SEARCH_QUERY_LENGTH characters"
        }

        return SearchQuery.of(cleanQuery)
    }

    /**
     * Reads an optional id filter.
     *
     * Identifiers are strings matched regardless of case, which the stores take care of, so there
     * is nothing to parse; see "Identifiers" in the OpenAPI contract.
     *
     * @param id Raw id value, or null when the caller supplied no filter.
     * @return Identifier, or null when the caller supplied no meaningful filter.
     */
    fun optionalId(id: String?): String? = id?.trim()?.ifBlank { null }

    /**
     * Bounds a list of ids.
     * @param ids Raw id values supplied by the caller.
     * @param argumentName Argument name used in validation messages.
     * @return The same identifiers.
     * @throws IllegalArgumentException When there are more ids than one query may load.
     */
    fun ids(ids: List<String>, argumentName: String): List<String> = limitedIds(ids, argumentName)

    /**
     * Parses a city id.
     *
     * Cities are the one resource identified by a number: they have no public code, and their
     * numbers are kept rather than replaced by names that change when a city is renamed.
     *
     * @param id Raw id value supplied by the caller.
     * @param argumentName Argument name used in validation messages.
     * @return Parsed identifier.
     * @throws IllegalArgumentException When the value is not a number.
     */
    fun cityId(id: String, argumentName: String): Long =
        id.trim().toLongOrNull() ?: throw IllegalArgumentException("$argumentName must be a numeric city ID")

    /**
     * Parses a bounded list of city ids.
     * @param ids Raw id values supplied by the caller.
     * @param argumentName Argument name used in validation messages.
     * @return Parsed identifiers.
     * @throws IllegalArgumentException When there are too many ids, or one is not a number.
     */
    fun cityIds(ids: List<String>, argumentName: String): List<Long> =
        limitedIds(ids, argumentName).map { cityId(it, argumentName) }

    private fun limitedIds(ids: List<String>, argumentName: String): List<String> {
        require(ids.size <= MAX_IDS_PER_QUERY) {
            "$argumentName cannot contain more than $MAX_IDS_PER_QUERY values"
        }

        return ids
    }
}
