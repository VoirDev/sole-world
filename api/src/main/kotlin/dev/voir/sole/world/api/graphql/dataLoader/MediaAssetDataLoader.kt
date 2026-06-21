package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.MediaAssetRepository
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.MediaAsset
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches media assets by media asset id.
 * @property mediaAssets Repository used to load media assets by id.
 */
@DgsDataLoader(name = MediaAssetDataLoader.DATA_LOADER_NAME)
class MediaAssetDataLoader(
    private val mediaAssets: MediaAssetRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<Long, MediaAsset> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<Long>): CompletionStage<Map<Long, MediaAsset>> {
        return CompletableFuture.supplyAsync({
            mediaAssets.getMediaAssetsByIds(keys.toList()).associate { asset ->
                asset.id to asset.toGql()
            }
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "MediaAssetDataLoader"
    }
}
