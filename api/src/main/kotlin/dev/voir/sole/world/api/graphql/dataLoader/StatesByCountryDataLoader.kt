package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.StateRepository
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.pageInfo
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.StatePage
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches state or province lists by country id.
 * @property states Repository used to load states or provinces grouped by country id.
 */
@DgsDataLoader(name = StatesByCountryDataLoader.DATA_LOADER_NAME)
class StatesByCountryDataLoader(
    private val states: StateRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<LocalizedCountryPageKey, StatePage> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedCountryPageKey>): CompletionStage<Map<LocalizedCountryPageKey, StatePage>> {
        return CompletableFuture.supplyAsync({
            keys.associateWith { key ->
                StatePage(
                    items = states.getStatesPageByCountryId(
                        countryId = key.countryId,
                        languageCode = key.languageCode,
                        query = key.query,
                        offset = key.pageRequest.offset,
                        limit = key.pageRequest.size,
                    ).map { it.toGql() },
                    pageInfo = pageInfo(
                        states.countStatesByCountryId(key.countryId, key.languageCode, key.query),
                        key.pageRequest,
                    ),
                )
            }
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "StatesByCountryDataLoader"
    }
}
