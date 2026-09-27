package dev.voir.sole.world.api.timezone

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.Timezone
import dev.voir.sole.world.graphql.dto.types.TimezonePage
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for timezones. */
@DgsComponent
class TimezoneGraphqlFetcher(
    private val timezoneStore: TimezoneStore,
) {
    /**
     * Lists or searches timezones.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @return Paginated timezone result.
     */
    @DgsQuery
    fun timezones(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): DataFetcherResult<TimezonePage> {
        val language = GraphqlRequest.language()
        val result = timezoneStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return GraphqlRequest.localized(
            TimezonePage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads timezones by id.
     * @param ids Timezone ids to load; at most 50.
     * @return Matching timezones.
     */
    @DgsQuery
    fun timezonesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Timezone>> {
        val language = GraphqlRequest.language()
        val timezones = timezoneStore.byIds(GraphqlRequest.ids(ids, "ids"), language)

        return GraphqlRequest.localized(timezones.map { it.toGql() }, language)
    }

    /**
     * Loads one timezone by id.
     * @param id Timezone id to load.
     * @return Matching timezone, or null when none exists.
     */
    @DgsQuery
    fun timezone(@InputArgument id: String): DataFetcherResult<Timezone> {
        val language = GraphqlRequest.language()
        val timezone = timezoneStore.byId(id, language)

        return GraphqlRequest.localized(timezone?.toGql(), language)
    }
}
