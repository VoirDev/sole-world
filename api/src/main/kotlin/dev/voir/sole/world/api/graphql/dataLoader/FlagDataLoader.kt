package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.FlagRepository
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.Flag
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches shared flags by flag id.
 * @property flags Repository used to load flags by id.
 */
@DgsDataLoader(name = FlagDataLoader.DATA_LOADER_NAME)
class FlagDataLoader(
    private val flags: FlagRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<Int, Flag> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<Int>): CompletionStage<Map<Int, Flag>> {
        return CompletableFuture.supplyAsync({
            flags.getFlagsByIds(keys.toList()).associate { flag ->
                flag.id to flag.toGql()
            }
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "FlagDataLoader"
    }
}
