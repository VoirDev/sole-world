package dev.voir.sole.world.api.currency

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.centralbank.toGql
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.dataset.index.SortOrder
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.CentralBank
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.Currency
import dev.voir.sole.world.graphql.dto.types.CurrencyPage
import dev.voir.sole.world.graphql.dto.types.Flag
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult
import dev.voir.sole.world.graphql.dto.types.CurrencySort as GqlCurrencySort
import dev.voir.sole.world.graphql.dto.types.SortOrder as GqlSortOrder

/** GraphQL fetchers for currencies and every field hanging off a currency. */
@DgsComponent
class CurrencyGraphqlFetcher(
    private val currencyStore: CurrencyStore,
    private val countryStore: CountryStore,
    private val centralBankStore: CentralBankStore,
    private val flagStore: FlagStore,
) {
    /**
     * Lists or searches currencies.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param obsolete Restricts results to obsolete or active currencies.
     * @param sort Field to order by; defaults to popularity, or to relevance when searching.
     * @param order Direction; defaults to the direction the chosen field is useful in.
     * @return Paginated currency result.
     */
    @DgsQuery
    fun currencies(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument obsolete: Boolean?,
        @InputArgument sort: GqlCurrencySort?,
        @InputArgument order: GqlSortOrder?,
    ): DataFetcherResult<CurrencyPage> {
        val language = GraphqlRequest.language()
        val result = currencyStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
            obsolete = obsolete,
            // The schema enums are generated from the same names the store's own enums use, so the
            // two stay in step: a value added to one does not compile until the other has it too.
            sort = sort?.let { CurrencySort.valueOf(it.name) },
            order = order?.let { SortOrder.valueOf(it.name) },
        )

        return GraphqlRequest.localized(
            CurrencyPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads currencies by id.
     * @param ids Currency ids to load; at most 50.
     * @return Matching currencies.
     */
    @DgsQuery
    fun currenciesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Currency>> {
        val language = GraphqlRequest.language()
        val currencies = currencyStore.byIds(GraphqlRequest.longIds(ids, "ids"), language)

        return GraphqlRequest.localized(currencies.map { it.toGql() }, language)
    }

    /**
     * Loads one currency by numeric identifier or ISO code.
     * @param idOrCode Numeric identifier, ISO 4217 alpha code, or ISO 4217 numeric code.
     * @param withObsolete Whether an obsolete currency may be returned.
     * @return Matching currency, or null when none matches.
     */
    @DgsQuery
    fun currency(
        @InputArgument idOrCode: String,
        @InputArgument withObsolete: Boolean?,
    ): DataFetcherResult<Currency> {
        val language = GraphqlRequest.language()
        val currency = currencyStore.resolve(idOrCode, withObsolete, language)

        return GraphqlRequest.localized(currency?.toGql(), language)
    }

    /** Resolves the currency that replaced an obsolete one. */
    @DgsData(parentType = "Currency", field = "replacedBy")
    fun replacedBy(dfe: DgsDataFetchingEnvironment): Currency? {
        val currency: Currency = dfe.getSource() ?: return null
        val replacedById = currency.replacedById ?: return null
        val language = GraphqlRequest.language(dfe)

        return currencyStore
            .byId(GraphqlRequest.longId(replacedById, "replacedById"), language)
            ?.toGql()
    }

    /** Resolves the countries that use a currency. */
    @DgsData(parentType = "Currency", field = "countries")
    fun countries(dfe: DgsDataFetchingEnvironment): List<Country>? {
        val currency: Currency = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return countryStore.byCurrencyId(currencyId(currency), language).map { it.toGql() }
    }

    /** Resolves the central banks that issue a currency. */
    @DgsData(parentType = "Currency", field = "centralBanks")
    fun centralBanks(dfe: DgsDataFetchingEnvironment): List<CentralBank>? {
        val currency: Currency = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return centralBankStore.byCurrencyId(currencyId(currency), language).map { it.toGql() }
    }

    /** Resolves the flag associated with a currency. */
    @DgsData(parentType = "Currency", field = "flag")
    fun flag(dfe: DgsDataFetchingEnvironment): Flag? {
        val currency: Currency = dfe.getSource() ?: return null
        val flagId = currency.flagId ?: return null

        return flagStore.byId(GraphqlRequest.longId(flagId, "flagId"))?.toGql()
    }

    private fun currencyId(currency: Currency): Long = GraphqlRequest.longId(currency.id, "id")
}
