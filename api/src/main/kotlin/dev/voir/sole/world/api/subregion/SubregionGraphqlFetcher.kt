package dev.voir.sole.world.api.subregion

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.region.RegionStore
import dev.voir.sole.world.api.region.toGql
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.Region
import dev.voir.sole.world.graphql.dto.types.Subregion
import dev.voir.sole.world.graphql.dto.types.SubregionPage
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for subregions and every field hanging off a subregion. */
@DgsComponent
class SubregionGraphqlFetcher(
    private val subregionStore: SubregionStore,
    private val regionStore: RegionStore,
    private val countryStore: CountryStore,
) {
    /**
     * Lists or searches subregions.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param regionId Restrict to subregions inside this region.
     * @return Paginated subregion result.
     */
    @DgsQuery
    fun subregions(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument regionId: String?,
    ): DataFetcherResult<SubregionPage> {
        val language = GraphqlRequest.language()
        val result = subregionStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
            regionId = GraphqlRequest.optionalLongId(regionId, "regionId"),
        )

        return GraphqlRequest.localized(
            SubregionPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads subregions by id.
     * @param ids Subregion ids to load; at most 50.
     * @return Matching subregions.
     */
    @DgsQuery
    fun subregionsByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Subregion>> {
        val language = GraphqlRequest.language()
        val subregions = subregionStore.byIds(GraphqlRequest.longIds(ids, "ids"), language)

        return GraphqlRequest.localized(subregions.map { it.toGql() }, language)
    }

    /**
     * Loads one subregion by id.
     * @param id Subregion id to load.
     * @return Matching subregion, or null when none exists.
     */
    @DgsQuery
    fun subregion(@InputArgument id: String): DataFetcherResult<Subregion> {
        val language = GraphqlRequest.language()
        val subregion = subregionStore.byId(GraphqlRequest.longId(id, "id"), language)

        return GraphqlRequest.localized(subregion?.toGql(), language)
    }

    /** Resolves the parent region of a subregion. */
    @DgsData(parentType = "Subregion", field = "region")
    fun region(dfe: DgsDataFetchingEnvironment): Region? {
        val subregion: Subregion = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return regionStore.byId(GraphqlRequest.longId(subregion.regionId, "regionId"), language)?.toGql()
    }

    /** Resolves the countries inside a subregion. */
    @DgsData(parentType = "Subregion", field = "countries")
    fun countries(dfe: DgsDataFetchingEnvironment): List<Country>? {
        val subregion: Subregion = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return countryStore
            .bySubregionId(GraphqlRequest.longId(subregion.id, "id"), language)
            .map { it.toGql() }
    }
}
