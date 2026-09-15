package dev.voir.sole.world.api.city

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.model.StateData
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.state.toGql
import dev.voir.sole.world.graphql.dto.types.City
import dev.voir.sole.world.graphql.dto.types.CityPage
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.State
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for cities and the records a city belongs to. */
@DgsComponent
class CityGraphqlFetcher(
    private val cityStore: CityStore,
    private val stateStore: StateStore,
    private val countryStore: CountryStore,
) {
    /**
     * Lists or searches cities.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param countryId Restrict to cities in this country.
     * @param stateId Restrict to cities in this state.
     * @return Paginated city result.
     */
    @DgsQuery
    fun cities(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument countryId: String?,
        @InputArgument stateId: String?,
    ): DataFetcherResult<CityPage> {
        val language = GraphqlRequest.language()
        val result = cityStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
            countryId = GraphqlRequest.optionalLongId(countryId, "countryId"),
            stateId = GraphqlRequest.optionalLongId(stateId, "stateId"),
        )

        return GraphqlRequest.localized(
            CityPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads cities by id.
     * @param ids City ids to load; at most 50.
     * @return Matching cities.
     */
    @DgsQuery
    fun citiesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<City>> {
        val language = GraphqlRequest.language()
        val cities = cityStore.byIds(GraphqlRequest.longIds(ids, "ids"), language)

        return GraphqlRequest.localized(cities.map { it.toGql() }, language)
    }

    /**
     * Loads one city by id.
     * @param id City id to load.
     * @return Matching city, or null when none exists.
     */
    @DgsQuery
    fun city(@InputArgument id: String): DataFetcherResult<City> {
        val language = GraphqlRequest.language()
        val city = cityStore.byId(GraphqlRequest.longId(id, "id"), language)

        return GraphqlRequest.localized(city?.toGql(), language)
    }

    /** Resolves the parent state of a city. */
    @DgsData(parentType = "City", field = "state")
    fun state(dfe: DgsDataFetchingEnvironment): State? = parentState(dfe)?.toGql()

    /** Resolves the country a city's parent state belongs to. */
    @DgsData(parentType = "City", field = "country")
    fun country(dfe: DgsDataFetchingEnvironment): Country? {
        // A city's country is reached through its state, which the dataset always provides.
        val state = parentState(dfe) ?: return null

        return countryStore.byId(state.countryId, GraphqlRequest.language(dfe))?.toGql()
    }

    private fun parentState(dfe: DgsDataFetchingEnvironment): StateData? {
        val city: City = dfe.getSource() ?: return null

        return stateStore.byId(GraphqlRequest.longId(city.stateId, "stateId"), GraphqlRequest.language(dfe))
    }
}
