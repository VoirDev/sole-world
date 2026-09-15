package dev.voir.sole.world.api.centralbank

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.currency.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.CentralBank
import dev.voir.sole.world.graphql.dto.types.CentralBankPage
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.Currency
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for central banks and the records hanging off one. */
@DgsComponent
class CentralBankGraphqlFetcher(
    private val centralBankStore: CentralBankStore,
    private val countryStore: CountryStore,
    private val currencyStore: CurrencyStore,
) {
    /**
     * Lists or searches central banks.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @return Paginated central bank result.
     */
    @DgsQuery
    fun centralBanks(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): DataFetcherResult<CentralBankPage> {
        val language = GraphqlRequest.language()
        val result = centralBankStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return GraphqlRequest.localized(
            CentralBankPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads central banks by id.
     * @param ids Central bank ids to load; at most 50.
     * @return Matching central banks.
     */
    @DgsQuery
    fun centralBanksByIds(@InputArgument ids: List<String>): DataFetcherResult<List<CentralBank>> {
        val language = GraphqlRequest.language()
        val banks = centralBankStore.byIds(GraphqlRequest.longIds(ids, "ids"), language)

        return GraphqlRequest.localized(banks.map { it.toGql() }, language)
    }

    /**
     * Loads one central bank by id.
     * @param id Central bank id to load.
     * @return Matching central bank, or null when none exists.
     */
    @DgsQuery
    fun centralBank(@InputArgument id: String): DataFetcherResult<CentralBank> {
        val language = GraphqlRequest.language()
        val bank = centralBankStore.byId(GraphqlRequest.longId(id, "id"), language)

        return GraphqlRequest.localized(bank?.toGql(), language)
    }

    /** Resolves the countries a central bank serves. */
    @DgsData(parentType = "CentralBank", field = "countries")
    fun countries(dfe: DgsDataFetchingEnvironment): List<Country>? {
        val bank: CentralBank = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return countryStore.byCentralBankId(bankId(bank), language).map { it.toGql() }
    }

    /** Resolves the currencies a central bank issues. */
    @DgsData(parentType = "CentralBank", field = "currencies")
    fun currencies(dfe: DgsDataFetchingEnvironment): List<Currency>? {
        val bank: CentralBank = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return currencyStore.byCentralBankId(bankId(bank), language).map { it.toGql() }
    }

    private fun bankId(bank: CentralBank): Long = GraphqlRequest.longId(bank.id, "id")
}
