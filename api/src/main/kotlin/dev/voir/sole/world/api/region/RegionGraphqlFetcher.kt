package dev.voir.sole.world.api.region

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.api.subregion.toGql
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.Region
import dev.voir.sole.world.graphql.dto.types.RegionPage
import dev.voir.sole.world.graphql.dto.types.Subregion
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for regions and every field hanging off a region. */
@DgsComponent
class RegionGraphqlFetcher(
    private val regionStore: RegionStore,
    private val subregionStore: SubregionStore,
    private val countryStore: CountryStore,
) {
    /**
     * Lists or searches regions.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @return Paginated region result.
     */
    @DgsQuery
    fun regions(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): DataFetcherResult<RegionPage> {
        val language = GraphqlRequest.language()
        val result = regionStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return GraphqlRequest.localized(
            RegionPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads regions by id.
     * @param ids Region ids to load; at most 50.
     * @return Matching regions.
     */
    @DgsQuery
    fun regionsByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Region>> {
        val language = GraphqlRequest.language()
        val regions = regionStore.byIds(GraphqlRequest.ids(ids, "ids"), language)

        return GraphqlRequest.localized(regions.map { it.toGql() }, language)
    }

    /**
     * Loads one region by id.
     * @param id Region id to load.
     * @return Matching region, or null when none exists.
     */
    @DgsQuery
    fun region(@InputArgument id: String): DataFetcherResult<Region> {
        val language = GraphqlRequest.language()
        val region = regionStore.byId(id, language)

        return GraphqlRequest.localized(region?.toGql(), language)
    }

    /** Resolves the subregions inside a region. */
    @DgsData(parentType = "Region", field = "subregions")
    fun subregions(dfe: DgsDataFetchingEnvironment): List<Subregion>? {
        val region: Region = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return subregionStore
            .byRegionId(region.id, language)
            .map { it.toGql() }
    }

    /** Resolves the countries inside a region. */
    @DgsData(parentType = "Region", field = "countries")
    fun countries(dfe: DgsDataFetchingEnvironment): List<Country>? {
        val region: Region = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return countryStore
            .byRegionId(region.id, language)
            .map { it.toGql() }
    }
}
