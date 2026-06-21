package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.CentralBankRepository
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.CentralBank
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches central bank lists by currency id.
 * @property centralBanks Repository used to load central banks grouped by currency id.
 */
@DgsDataLoader(name = CentralBanksByCurrencyDataLoader.DATA_LOADER_NAME)
class CentralBanksByCurrencyDataLoader(
    private val centralBanks: CentralBankRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<LocalizedIntKey, List<CentralBank>> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedIntKey>): CompletionStage<Map<LocalizedIntKey, List<CentralBank>>> {
        return CompletableFuture.supplyAsync({
            val groupedResults = keys.groupBy { it.languageCode }.mapValues { (languageCode, localizedKeys) ->
                centralBanks.getCentralBanksByCurrencyIds(
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
        const val DATA_LOADER_NAME = "CentralBanksByCurrencyDataLoader"
    }
}
