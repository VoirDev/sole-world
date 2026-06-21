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
 * DataLoader that batches subregion lists by region id.
 * @property regions Repository used to load subregions grouped by region id.
 */
@DgsDataLoader(name = SubregionsByRegionDataLoader.DATA_LOADER_NAME)
class SubregionsByRegionDataLoader(
    private val regions: RegionRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<LocalizedIntKey, List<Subregion>> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedIntKey>): CompletionStage<Map<LocalizedIntKey, List<Subregion>>> {
        return CompletableFuture.supplyAsync({
            val groupedResults = keys.groupBy { it.languageCode }.mapValues { (languageCode, localizedKeys) ->
                regions.getSubregionsGroupedByRegionIds(
                    ids = localizedKeys.map { it.id }.toSet(),
                    languageCode = languageCode,
                )
            }

            keys.associateWith { key ->
                groupedResults[key.languageCode]?.get(key.id)
                    ?.map { it.toGql() }
                    ?: emptyList()
            }
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "SubregionsByRegionDataLoader"
    }
}
