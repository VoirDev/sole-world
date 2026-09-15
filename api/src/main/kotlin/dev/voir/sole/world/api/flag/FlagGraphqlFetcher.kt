package dev.voir.sole.world.api.flag

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.mediaasset.MediaAssetStore
import dev.voir.sole.world.api.mediaasset.toGql
import dev.voir.sole.world.graphql.dto.types.Flag
import dev.voir.sole.world.graphql.dto.types.FlagPage
import dev.voir.sole.world.graphql.dto.types.MediaAsset
import dev.voir.sole.world.graphql.dto.types.PageInput

/** GraphQL fetchers for flags and the media assets they point at. */
@DgsComponent
class FlagGraphqlFetcher(
    private val flagStore: FlagStore,
    private val mediaAssetStore: MediaAssetStore,
) {
    /**
     * Lists or searches flags.
     * @param page Optional pagination request.
     * @param query Optional relevance search over captions and emoji.
     * @return Paginated flag result.
     */
    @DgsQuery
    fun flags(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): FlagPage {
        val result = flagStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = GraphqlRequest.language(),
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return FlagPage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }

    /**
     * Loads flags by id.
     * @param ids Flag ids to load; at most 50.
     * @return Matching flags.
     */
    @DgsQuery
    fun flagsByIds(@InputArgument ids: List<String>): List<Flag> =
        flagStore.byIds(GraphqlRequest.longIds(ids, "ids")).map { it.toGql() }

    /**
     * Loads one flag by id.
     * @param id Flag id to load.
     * @return Matching flag, or null when none exists.
     */
    @DgsQuery
    fun flag(@InputArgument id: String): Flag? =
        flagStore.byId(GraphqlRequest.longId(id, "id"))?.toGql()

    /** Resolves the square media asset of a flag. */
    @DgsData(parentType = "Flag", field = "squareAsset")
    fun squareAsset(dfe: DgsDataFetchingEnvironment): MediaAsset? = asset(dfe) { it.squareAssetId }

    /** Resolves the wide media asset of a flag. */
    @DgsData(parentType = "Flag", field = "wideAsset")
    fun wideAsset(dfe: DgsDataFetchingEnvironment): MediaAsset? = asset(dfe) { it.wideAssetId }

    private fun asset(dfe: DgsDataFetchingEnvironment, assetId: (Flag) -> String?): MediaAsset? {
        val flag: Flag = dfe.getSource() ?: return null
        val id = assetId(flag) ?: return null

        return mediaAssetStore.byId(GraphqlRequest.longId(id, "assetId"))?.toGql()
    }
}
