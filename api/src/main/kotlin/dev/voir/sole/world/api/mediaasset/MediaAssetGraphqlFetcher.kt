package dev.voir.sole.world.api.mediaasset

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.MediaAsset
import dev.voir.sole.world.graphql.dto.types.MediaAssetPage
import dev.voir.sole.world.graphql.dto.types.PageInput

/** GraphQL fetchers for media asset metadata. */
@DgsComponent
class MediaAssetGraphqlFetcher(
    private val mediaAssetStore: MediaAssetStore,
) {
    /**
     * Lists media assets using zero-based pagination.
     * @param page Optional pagination request.
     * @return Paginated media asset result.
     */
    @DgsQuery
    fun mediaAssets(@InputArgument page: PageInput?): MediaAssetPage {
        val result = mediaAssetStore.page(GraphqlRequest.pageRequest(page))

        return MediaAssetPage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }

    /**
     * Loads media assets by id.
     * @param ids Media asset ids to load; at most 50.
     * @return Matching media assets.
     */
    @DgsQuery
    fun mediaAssetsByIds(@InputArgument ids: List<String>): List<MediaAsset> =
        mediaAssetStore.byIds(GraphqlRequest.longIds(ids, "ids")).map { it.toGql() }

    /**
     * Loads one media asset by id.
     * @param id Media asset id to load.
     * @return Matching media asset, or null when none exists.
     */
    @DgsQuery
    fun mediaAsset(@InputArgument id: String): MediaAsset? =
        mediaAssetStore.byId(GraphqlRequest.longId(id, "id"))?.toGql()
}
