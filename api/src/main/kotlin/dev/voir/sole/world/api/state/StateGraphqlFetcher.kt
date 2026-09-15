package dev.voir.sole.world.api.state

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.city.toGql
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.CityPage
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.State
import dev.voir.sole.world.graphql.dto.types.StatePage
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for states and provinces, and the records hanging off one. */
@DgsComponent
class StateGraphqlFetcher(
    private val stateStore: StateStore,
    private val cityStore: CityStore,
    private val countryStore: CountryStore,
) {
    /**
     * Lists or searches states.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param countryId Restrict to states in this country.
     * @return Paginated state result.
     */
    @DgsQuery
    fun states(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument countryId: String?,
    ): DataFetcherResult<StatePage> {
        val language = GraphqlRequest.language()
        val result = stateStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
            countryId = GraphqlRequest.optionalLongId(countryId, "countryId"),
        )

        return GraphqlRequest.localized(
            StatePage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads states by id.
     * @param ids State ids to load; at most 50.
     * @return Matching states.
     */
    @DgsQuery
    fun statesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<State>> {
        val language = GraphqlRequest.language()
        val states = stateStore.byIds(GraphqlRequest.longIds(ids, "ids"), language)

        return GraphqlRequest.localized(states.map { it.toGql() }, language)
    }

    /**
     * Loads one state by id.
     * @param id State id to load.
     * @return Matching state, or null when none exists.
     */
    @DgsQuery
    fun state(@InputArgument id: String): DataFetcherResult<State> {
        val language = GraphqlRequest.language()
        val state = stateStore.byId(GraphqlRequest.longId(id, "id"), language)

        return GraphqlRequest.localized(state?.toGql(), language)
    }

    /** Resolves the parent country of a state. */
    @DgsData(parentType = "State", field = "country")
    fun country(dfe: DgsDataFetchingEnvironment): Country? {
        val state: State = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return countryStore.byId(GraphqlRequest.longId(state.countryId, "countryId"), language)?.toGql()
    }

    /**
     * Resolves a page of a state's cities.
     * @param page Optional pagination request.
     * @param query Optional case-insensitive name filter.
     */
    @DgsData(parentType = "State", field = "cities")
    fun cities(
        dfe: DgsDataFetchingEnvironment,
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): CityPage? {
        val state: State = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        val result = cityStore.pageByStateId(
            stateId = GraphqlRequest.longId(state.id, "id"),
            request = GraphqlRequest.pageRequest(page),
            query = GraphqlRequest.optionalSearchQuery(query),
            languageCode = language,
        )

        return CityPage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }
}
