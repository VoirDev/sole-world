package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.CountryRepository
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.Country
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches country lists by language id.
 * @property countries Repository used to load countries grouped by language id.
 */
@DgsDataLoader(name = CountriesByLanguageDataLoader.DATA_LOADER_NAME)
class CountriesByLanguageDataLoader(
    private val countries: CountryRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<LocalizedIntKey, List<Country>> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedIntKey>): CompletionStage<Map<LocalizedIntKey, List<Country>>> {
        return CompletableFuture.supplyAsync({
            val groupedResults = keys.groupBy { it.languageCode }.mapValues { (languageCode, localizedKeys) ->
                countries.getCountriesByLanguageIds(
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
        const val DATA_LOADER_NAME = "CountriesByLanguageDataLoader"
    }
}
