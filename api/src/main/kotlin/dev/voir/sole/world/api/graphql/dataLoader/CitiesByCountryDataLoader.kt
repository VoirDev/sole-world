package dev.voir.sole.world.api.graphql.dataLoader

import com.netflix.graphql.dgs.DgsDataLoader
import dev.voir.sole.world.api.database.CityRepository
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.pageInfo
import dev.voir.sole.world.api.graphql.toGql
import dev.voir.sole.world.graphql.dto.types.CityPage
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Qualifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * DataLoader that batches city lists by country id.
 * @property cities Repository used to load cities grouped by country id.
 */
@DgsDataLoader(name = CitiesByCountryDataLoader.DATA_LOADER_NAME)
class CitiesByCountryDataLoader(
    private val cities: CityRepository,
    @Qualifier("graphqlDataLoaderExecutor")
    private val graphqlDataLoaderExecutor: Executor,
) : MappedBatchLoader<LocalizedCountryPageKey, CityPage> {

    /**
     * Loads a batch of GraphQL field values for the requested keys.
     * @param keys Batch keys requested by DataLoader.
     * @return Completion stage containing values keyed by the requested ids.
     */
    override fun load(keys: Set<LocalizedCountryPageKey>): CompletionStage<Map<LocalizedCountryPageKey, CityPage>> {
        return CompletableFuture.supplyAsync({
            keys.associateWith { key ->
                CityPage(
                    items = cities.getCitiesPageByCountryId(
                        countryId = key.countryId,
                        languageCode = key.languageCode,
                        query = key.query,
                        offset = key.pageRequest.offset,
                        limit = key.pageRequest.size,
                    ).map { it.toGql() },
                    pageInfo = pageInfo(
                        cities.countCitiesByCountryId(key.countryId, key.languageCode, key.query),
                        key.pageRequest,
                    ),
                )
            }
        }, graphqlDataLoaderExecutor)
    }

    companion object {
        const val DATA_LOADER_NAME = "CitiesByCountryDataLoader"
    }
}
