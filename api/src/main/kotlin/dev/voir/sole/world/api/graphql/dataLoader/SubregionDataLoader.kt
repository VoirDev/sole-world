package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.RegionRepository
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.Subregion
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches subregions by subregion id.
 * @property regions Repository used to load regions and subregions by id.
 */
@DgsDataLoader(name = SubregionDataLoader.DATA_LOADER_NAME)
class SubregionDataLoader(
    private val regions: RegionRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) :
    MappedBatchLoader<LocalizedIntKey, Subregion> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedIntKey>): CompletionStage<Map<LocalizedIntKey, Subregion>> {
        return CompletableFuture.supplyAsync({
            keys.groupBy { it.languageCode }
                .flatMap { (languageCode, localizedKeys) ->
                    regions.getSubregionsByIds(
                        ids = localizedKeys.map { it.id },
                        languageCode = languageCode,
                    ).map { subregion ->
                        LocalizedIntKey(subregion.id, languageCode) to subregion.toGql()
                    }
                }
                .toMap()
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "SubregionDataLoader"
    }
}
