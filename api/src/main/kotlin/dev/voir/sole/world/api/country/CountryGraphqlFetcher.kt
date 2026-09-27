package dev.voir.sole.world.api.country

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.centralbank.toGql
import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.city.toGql
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.currency.toGql
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.language.toGql
import dev.voir.sole.world.api.region.RegionStore
import dev.voir.sole.world.api.region.toGql
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.state.toGql
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.api.subregion.toGql
import dev.voir.sole.world.api.timezone.TimezoneStore
import dev.voir.sole.world.api.timezone.toGql
import dev.voir.sole.world.graphql.dto.types.CentralBank
import dev.voir.sole.world.graphql.dto.types.CityPage
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.CountryPage
import dev.voir.sole.world.graphql.dto.types.Currency
import dev.voir.sole.world.graphql.dto.types.Flag
import dev.voir.sole.world.graphql.dto.types.Language
import dev.voir.sole.world.graphql.dto.types.PageInput
import dev.voir.sole.world.graphql.dto.types.Region
import dev.voir.sole.world.graphql.dto.types.StatePage
import dev.voir.sole.world.graphql.dto.types.Subregion
import dev.voir.sole.world.graphql.dto.types.Timezone
import graphql.execution.DataFetcherResult

/**
 * GraphQL fetchers for countries and every field hanging off a country.
 *
 * Relationship fields read directly from their owning store. There are no DataLoaders: every lookup
 * is an in-memory index hit, so there is no round trip left to batch away.
 */
@DgsComponent
class CountryGraphqlFetcher(
    private val countryStore: CountryStore,
    private val regionStore: RegionStore,
    private val subregionStore: SubregionStore,
    private val currencyStore: CurrencyStore,
    private val timezoneStore: TimezoneStore,
    private val languageStore: LanguageStore,
    private val centralBankStore: CentralBankStore,
    private val stateStore: StateStore,
    private val cityStore: CityStore,
    private val flagStore: FlagStore,
) {
    /**
     * Lists or searches countries.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @param regionId Restrict to countries in this region.
     * @param subregionId Restrict to countries in this subregion.
     * @param currencyId Restrict to countries that use this currency.
     * @param languageId Restrict to countries that use this language.
     * @param timezoneId Restrict to countries in this timezone.
     * @return Paginated country result.
     */
    @DgsQuery
    fun countries(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
        @InputArgument regionId: String?,
        @InputArgument subregionId: String?,
        @InputArgument currencyId: String?,
        @InputArgument languageId: String?,
        @InputArgument timezoneId: String?,
    ): DataFetcherResult<CountryPage> {
        val language = GraphqlRequest.language()
        val result = countryStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
            regionId = GraphqlRequest.optionalId(regionId),
            subregionId = GraphqlRequest.optionalId(subregionId),
            currencyId = GraphqlRequest.optionalId(currencyId),
            languageId = GraphqlRequest.optionalId(languageId),
            timezoneId = GraphqlRequest.optionalId(timezoneId),
        )

        return GraphqlRequest.localized(
            CountryPage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads countries by id.
     * @param ids Country ids to load; at most 50.
     * @return Matching countries.
     */
    @DgsQuery
    fun countriesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Country>> {
        val language = GraphqlRequest.language()
        val countries = countryStore.byIds(GraphqlRequest.ids(ids, "ids"), language)

        return GraphqlRequest.localized(countries.map { it.toGql() }, language)
    }

    /**
     * Loads one country by its identifier or one of its other ISO 3166-1 codes.
     * @param id Alpha-2 identifier, alpha-3 code, or numeric code, in any case.
     * @return Matching country, or null when none exists.
     */
    @DgsQuery
    fun country(@InputArgument id: String): DataFetcherResult<Country> {
        val language = GraphqlRequest.language()

        return GraphqlRequest.localized(countryStore.byIdentifier(id, language)?.toGql(), language)
    }

    /** Resolves the parent region of a country. */
    @DgsData(parentType = "Country", field = "region")
    fun region(dfe: DgsDataFetchingEnvironment): Region? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return regionStore.byId(country.regionId, language)?.toGql()
    }

    /** Resolves the parent subregion of a country. */
    @DgsData(parentType = "Country", field = "subregion")
    fun subregion(dfe: DgsDataFetchingEnvironment): Subregion? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return subregionStore.byId(country.subregionId, language)?.toGql()
    }

    /** Resolves the currencies used by a country. */
    @DgsData(parentType = "Country", field = "currencies")
    fun currencies(dfe: DgsDataFetchingEnvironment): List<Currency>? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return currencyStore.byCountryId(country.id, language).map { it.toGql() }
    }

    /** Resolves the timezones associated with a country. */
    @DgsData(parentType = "Country", field = "timezones")
    fun timezones(dfe: DgsDataFetchingEnvironment): List<Timezone>? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return timezoneStore.byCountryId(country.id, language).map { it.toGql() }
    }

    /** Resolves the languages associated with a country. */
    @DgsData(parentType = "Country", field = "languages")
    fun languages(dfe: DgsDataFetchingEnvironment): List<Language>? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return languageStore.byCountryId(country.id, language).map { it.toGql() }
    }

    /** Resolves the central banks serving a country. */
    @DgsData(parentType = "Country", field = "centralBanks")
    fun centralBanks(dfe: DgsDataFetchingEnvironment): List<CentralBank>? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return centralBankStore.byCountryId(country.id, language).map { it.toGql() }
    }

    /** Resolves the flag of a country. */
    @DgsData(parentType = "Country", field = "flag")
    fun flag(dfe: DgsDataFetchingEnvironment): Flag? {
        val country: Country = dfe.getSource() ?: return null
        val flagId = country.flagId ?: return null

        return flagStore.byId(flagId)?.toGql()
    }

    /**
     * Resolves a page of a country's states.
     * @param page Optional pagination request.
     * @param query Optional case-insensitive name filter.
     */
    @DgsData(parentType = "Country", field = "states")
    fun states(
        dfe: DgsDataFetchingEnvironment,
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): StatePage? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        val result = stateStore.pageByCountryId(
            countryId = country.id,
            request = GraphqlRequest.pageRequest(page),
            query = GraphqlRequest.optionalSearchQuery(query),
            languageCode = language,
        )

        return StatePage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }

    /**
     * Resolves a page of a country's cities.
     * @param page Optional pagination request.
     * @param query Optional case-insensitive name filter.
     */
    @DgsData(parentType = "Country", field = "cities")
    fun cities(
        dfe: DgsDataFetchingEnvironment,
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): CityPage? {
        val country: Country = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        val result = cityStore.pageByCountryId(
            countryId = country.id,
            request = GraphqlRequest.pageRequest(page),
            query = GraphqlRequest.optionalSearchQuery(query),
            languageCode = language,
        )

        return CityPage(
            items = result.items.map { it.toGql() },
            pageInfo = GraphqlRequest.pageInfo(result.metadata),
        )
    }
}
