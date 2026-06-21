package dev.voir.sole.world.api.graphql

import com.netflix.graphql.dgs.*
import dev.voir.sole.world.api.database.*
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.acceptLanguageCode
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.intId
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.intIds
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.limitedResultCount
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.localizedIntKey
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.localizedResult
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.longId
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.longIds
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.optionalSearchQuery
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.pageInfo
import dev.voir.sole.world.api.graphql.GraphqlRequestUtils.pageRequest
import dev.voir.sole.world.api.graphql.dataLoader.*
import dev.voir.sole.world.api.graphql.security.Authenticated
import dev.voir.sole.world.graphql.dto.types.*
import graphql.execution.DataFetcherResult
import java.util.concurrent.CompletableFuture

@DgsComponent
class Query(
    private val countryRepository: CountryRepository,
    private val currencyRepository: CurrencyRepository,
    private val languageRepository: LanguageRepository,
    private val flagRepository: FlagRepository,
    private val regionRepository: RegionRepository,
    private val timezoneRepository: TimezoneRepository,
    private val centralBankRepository: CentralBankRepository,
    private val stateRepository: StateRepository,
    private val cityRepository: CityRepository,
    private val mediaAssetRepository: MediaAssetRepository,
) {
    /**
     * Lists countries using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated country result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listCountries(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<CountryPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(CountryPage(
            items = countryRepository
                .getCountriesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(countryRepository.countCountries(), request),
        ), language)
    }

    /**
     * Resolves countries.
     * @param ids Country ids to load. At most 50 ids are allowed.
     * @return Countries mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun countries(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Country>> {
        val language = acceptLanguageCode()

        return localizedResult(countryRepository
            .getCountriesByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Searches countries by name, ISO code, phone code, and top-level domain.
     * @param query Search text.
     * Uses the Accept-Language request header to select translated fields.
     * @param limit Optional result cap; defaults to 50 and cannot exceed 50.
     * @return Matching countries mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun searchCountries(
        @InputArgument query: String,
        @InputArgument limit: Int?,
    ): DataFetcherResult<List<Country>> {
        val cleanQuery = query.trim()
        val resultLimit = limitedResultCount(limit)
        val language = acceptLanguageCode()

        require(cleanQuery.isNotBlank()) { "query must not be blank" }

        return localizedResult(countryRepository
            .searchCountries(query = cleanQuery, languageCode = language, limit = resultLimit)
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single country by id.
     * @param id Country id to load.
     * @return Matching country, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun country(
        @InputArgument id: String,
    ): DataFetcherResult<Country> {
        val language = acceptLanguageCode()

        return localizedResult(countryRepository
            .getCountryById(id = intId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Lists currencies using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated currency result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listCurrencies(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<CurrencyPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(CurrencyPage(
            items = currencyRepository
                .getCurrenciesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(currencyRepository.countCurrencies(), request),
        ), language)
    }

    /**
     * Resolves the parent region for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing the parent region.
     */
    @DgsData(parentType = "Country", field = "region")
    fun countryRegion(dfe: DgsDataFetchingEnvironment): CompletableFuture<Region> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, Region>(RegionDataLoader.DATA_LOADER_NAME)!!

        return loader.load(localizedIntKey(country.regionId, "regionId", dfe))
    }

    /**
     * Resolves the parent subregion for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing the parent subregion.
     */
    @DgsData(parentType = "Country", field = "subregion")
    fun countrySubregion(dfe: DgsDataFetchingEnvironment): CompletableFuture<Subregion> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, Subregion>(SubregionDataLoader.DATA_LOADER_NAME)!!

        return loader.load(localizedIntKey(country.subregionId, "subregionId", dfe))
    }

    /**
     * Resolves currencies for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing currencies used by the country.
     */
    @DgsData(parentType = "Country", field = "currencies")
    fun countryCurrencies(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Currency>> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Currency>>(
            CurrenciesByCountryDataLoader.DATA_LOADER_NAME
        )!!
        return loader.load(localizedIntKey(country.id, "id", dfe))
    }

    /**
     * Resolves timezones for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing timezones associated with the country.
     */
    @DgsData(parentType = "Country", field = "timezones")
    fun countryTimezones(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Timezone>> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Timezone>>(
            TimezonesByCountryDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(country.id, "id", dfe))
    }

    /**
     * Resolves languages for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing languages associated with the country.
     */
    @DgsData(parentType = "Country", field = "languages")
    fun countryLanguages(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Language>> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Language>>(
            LanguagesByCountryDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(country.id, "id", dfe))
    }

    /**
     * Resolves central banks for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing central banks serving the country.
     */
    @DgsData(parentType = "Country", field = "centralBanks")
    fun countryCentralBanks(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<CentralBank>> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<CentralBank>>(
            CentralBanksByCountryDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(country.id, "id", dfe))
    }

    /**
     * Resolves states or provinces for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing states or provinces in the country.
     */
    @DgsData(parentType = "Country", field = "states")
    fun countryStates(
        dfe: DgsDataFetchingEnvironment,
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): CompletableFuture<StatePage> {
        val country: Country = dfe.getSource() ?: return CompletableFuture.completedFuture(null)
        val request = pageRequest(page)
        val cleanQuery = optionalSearchQuery(query)

        val loader = dfe.getDataLoader<LocalizedCountryPageKey, StatePage>(
            StatesByCountryDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(
            LocalizedCountryPageKey(
                countryId = intId(country.id, "id"),
                languageCode = dfe.getLocalContext<String>(),
                pageRequest = request,
                query = cleanQuery,
            )
        )
    }

    /**
     * Resolves cities for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing cities in the country.
     */
    @DgsData(parentType = "Country", field = "cities")
    fun countryCities(
        dfe: DgsDataFetchingEnvironment,
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): CompletableFuture<CityPage> {
        val country: Country = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)
        val request = pageRequest(page)
        val cleanQuery = optionalSearchQuery(query)

        val loader = dfe.getDataLoader<LocalizedCountryPageKey, CityPage>(
            CitiesByCountryDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(
            LocalizedCountryPageKey(
                countryId = intId(country.id, "id"),
                languageCode = dfe.getLocalContext<String>(),
                pageRequest = request,
                query = cleanQuery,
            )
        )
    }

    /**
     * Resolves the flag for a country via DataLoader.
     * @param dfe DGS environment containing the source country.
     * @return Future containing the flag, or null.
     */
    @DgsData(parentType = "Country", field = "flag")
    fun countryFlag(dfe: DgsDataFetchingEnvironment): CompletableFuture<Flag> {
        val country: Country = dfe.getSource() ?: return CompletableFuture.completedFuture(null)

        val id = country.flagId ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<Int, Flag>(FlagDataLoader.DATA_LOADER_NAME)!!

        return loader.load(intId(id, "flagId"))
    }

    /**
     * Resolves currencies.
     * @param ids Currency ids to load. At most 50 ids are allowed.
     * @return Currencies mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun currencies(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Currency>> {
        val language = acceptLanguageCode()

        return localizedResult(currencyRepository
            .getCurrenciesByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single currency by id.
     * @param id Currency id to load.
     * @return Matching currency, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun currency(
        @InputArgument id: String,
    ): DataFetcherResult<Currency> {
        val language = acceptLanguageCode()

        return localizedResult(currencyRepository
            .getCurrencyById(id = intId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Resolves a currency by ISO alpha or numeric identifier.
     * @param identifier ISO alpha or numeric currency identifier.
     * @param withObsolete When true, obsolete currencies may be returned.
     * @return Matching currency, or null when no match exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun resolveCurrency(
        @InputArgument identifier: String,
        @InputArgument withObsolete: Boolean?
    ): Currency? {
        return currencyRepository.resolveCurrency(
            identifier = identifier,
            withObsolete = withObsolete
        )?.toGql()
    }

    /**
     * Resolves countries that use a currency via DataLoader.
     * @param dfe DGS environment containing the source currency.
     * @return Future containing countries using the currency.
     */
    @DgsData(parentType = "Currency", field = "countries")
    fun currencyCountries(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Country>> {
        val currency: Currency = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Country>>(
            CountriesByCurrencyDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(currency.id, "id", dfe))
    }

    /**
     * Resolves central banks that issue a currency via DataLoader.
     * @param dfe DGS environment containing the source currency.
     * @return Future containing central banks issuing the currency.
     */
    @DgsData(parentType = "Currency", field = "centralBanks")
    fun currencyCentralBanks(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<CentralBank>> {
        val currency: Currency = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<CentralBank>>(
            CentralBanksByCurrencyDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(currency.id, "id", dfe))
    }

    /**
     * Resolves the flag for a currency via DataLoader.
     * @param dfe DGS environment containing the source currency.
     * @return Future containing the flag, or null.
     */
    @DgsData(parentType = "Currency", field = "flag")
    fun currencyFlag(dfe: DgsDataFetchingEnvironment): CompletableFuture<Flag> {
        val currency: Currency = dfe.getSource() ?: return CompletableFuture.completedFuture(null)
        val id = currency.flagId ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<Int, Flag>(FlagDataLoader.DATA_LOADER_NAME)!!

        return loader.load(intId(id, "flagId"))
    }

    /**
     * Lists languages using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated language result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listLanguages(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<LanguagePage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(LanguagePage(
            items = languageRepository
                .getLanguagesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(languageRepository.countLanguages(), request),
        ), language)
    }

    /**
     * Resolves languages.
     * @param ids Language ids to load. At most 50 ids are allowed.
     * @return Languages mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun languages(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Language>> {
        val language = acceptLanguageCode()

        return localizedResult(languageRepository
            .getLanguagesByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single language by id.
     * @param id Language id to load.
     * @return Matching language, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun language(
        @InputArgument id: String,
    ): DataFetcherResult<Language> {
        val language = acceptLanguageCode()

        return localizedResult(languageRepository
            .getLanguagesByIds(
                ids = listOf(intId(id, "id")),
                languageCode = language
            )
            .firstOrNull()
            ?.toGql(), language)
    }

    /**
     * Resolves shared flags.
     * @param ids Flag ids to load. At most 50 ids are allowed.
     * @return Flags mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun flags(@InputArgument ids: List<String>): List<Flag> {
        return flagRepository.getFlagsByIds(ids = intIds(ids, "ids")).map { it.toGql() }
    }

    /**
     * Resolves a single shared flag by id.
     * @param id Flag id to load.
     * @return Matching flag, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun flag(@InputArgument id: String): Flag? {
        return flagRepository.getFlagsByIds(ids = listOf(intId(id, "id"))).firstOrNull()?.toGql()
    }

    /**
     * Lists flags using zero-based pagination.
     * @param page Optional pagination request.
     * @return Paginated flag result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listFlags(@InputArgument page: PageInput?): FlagPage {
        val request = pageRequest(page)

        return FlagPage(
            items = flagRepository.getFlagsPage(request.offset, request.size).map { it.toGql() },
            pageInfo = pageInfo(flagRepository.countFlags(), request),
        )
    }

    /**
     * Resolves countries that use a language via DataLoader.
     * @param dfe DGS environment containing the source language.
     * @return Future containing countries using the language.
     */
    @DgsData(parentType = "Language", field = "countries")
    fun languageCountries(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Country>> {
        val language: Language = dfe.getSource() ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Country>>(
            CountriesByLanguageDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(language.id, "id", dfe))
    }

    /**
     * Resolves the flag for a language via DataLoader.
     * @param dfe DGS environment containing the source language.
     * @return Future containing the flag, or null.
     */
    @DgsData(parentType = "Language", field = "flag")
    fun languageFlag(dfe: DgsDataFetchingEnvironment): CompletableFuture<Flag> {
        val language: Language = dfe.getSource() ?: return CompletableFuture.completedFuture(null)
        val id = language.flagId ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<Int, Flag>(FlagDataLoader.DATA_LOADER_NAME)!!

        return loader.load(intId(id, "flagId"))
    }

    /**
     * Resolves the square media asset for a flag via DataLoader.
     * @param dfe DGS environment containing the source flag.
     * @return Future containing the square media asset, or null.
     */
    @DgsData(parentType = "Flag", field = "squareAsset")
    fun flagSquareAsset(dfe: DgsDataFetchingEnvironment): CompletableFuture<MediaAsset> {
        val flag: Flag = dfe.getSource() ?: return CompletableFuture.completedFuture(null)
        val id = flag.squareAssetId ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<Long, MediaAsset>(MediaAssetDataLoader.DATA_LOADER_NAME)!!

        return loader.load(longId(id, "squareAssetId"))
    }

    /**
     * Resolves the wide media asset for a flag via DataLoader.
     * @param dfe DGS environment containing the source flag.
     * @return Future containing the wide media asset, or null.
     */
    @DgsData(parentType = "Flag", field = "wideAsset")
    fun flagWideAsset(dfe: DgsDataFetchingEnvironment): CompletableFuture<MediaAsset> {
        val flag: Flag = dfe.getSource() ?: return CompletableFuture.completedFuture(null)
        val id = flag.wideAssetId ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<Long, MediaAsset>(MediaAssetDataLoader.DATA_LOADER_NAME)!!

        return loader.load(longId(id, "wideAssetId"))
    }

    /**
     * Lists regions using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated region result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listRegions(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<RegionPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(RegionPage(
            items = regionRepository
                .getRegionsPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(regionRepository.countRegions(), request),
        ), language)
    }

    /**
     * Resolves regions.
     * @param ids Region ids to load. At most 50 ids are allowed.
     * @return Regions mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun regions(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Region>> {
        val language = acceptLanguageCode()

        return localizedResult(regionRepository
            .getRegionsByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single region by id.
     * @param id Region id to load.
     * @return Matching region, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun region(
        @InputArgument id: String,
    ): DataFetcherResult<Region> {
        val language = acceptLanguageCode()

        return localizedResult(regionRepository
            .getRegionById(id = intId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Resolves subregions nested under a region via DataLoader.
     * @param dfe DGS environment containing the source region.
     * @return Future containing subregions in the region.
     */
    @DgsData(parentType = "Region", field = "subregions")
    fun regionSubregions(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Subregion>> {
        val region: Region = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Subregion>>(
            SubregionsByRegionDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(region.id, "id", dfe))
    }

    /**
     * Resolves countries nested under a region via DataLoader.
     * @param dfe DGS environment containing the source region.
     * @return Future containing countries in the region.
     */
    @DgsData(parentType = "Region", field = "countries")
    fun regionCountries(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Country>> {
        val region: Region = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Country>>(
            CountriesByRegionDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(region.id, "id", dfe))
    }

    /**
     * Lists subregions using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated subregion result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listSubregions(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<SubregionPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(SubregionPage(
            items = regionRepository
                .getSubregionsPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(regionRepository.countSubregions(), request),
        ), language)
    }

    /**
     * Resolves subregions.
     * @param ids Subregion ids to load. At most 50 ids are allowed.
     * @return Subregions mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun subregions(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Subregion>> {
        val language = acceptLanguageCode()

        return localizedResult(regionRepository
            .getSubregionsByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single subregion by id.
     * @param id Subregion id to load.
     * @return Matching subregion, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun subregion(
        @InputArgument id: String,
    ): DataFetcherResult<Subregion> {
        val language = acceptLanguageCode()

        return localizedResult(regionRepository
            .getSubregionById(id = intId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Resolves the parent region for a subregion via DataLoader.
     * @param dfe DGS environment containing the source subregion.
     * @return Future containing the parent region.
     */
    @DgsData(parentType = "Subregion", field = "region")
    fun subregionRegion(dfe: DgsDataFetchingEnvironment): CompletableFuture<Region> {
        val subregion: Subregion = dfe.getSource() ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, Region>(RegionDataLoader.DATA_LOADER_NAME)!!

        return loader.load(localizedIntKey(subregion.regionId, "regionId", dfe))
    }

    /**
     * Resolves countries nested under a subregion via DataLoader.
     * @param dfe DGS environment containing the source subregion.
     * @return Future containing countries in the subregion.
     */
    @DgsData(parentType = "Subregion", field = "countries")
    fun subregionCountries(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<Country>> {
        val subregion: Subregion = dfe.getSource()
            ?: return CompletableFuture.completedFuture(null)

        val loader = dfe.getDataLoader<LocalizedIntKey, List<Country>>(
            CountriesBySubregionDataLoader.DATA_LOADER_NAME
        )!!

        return loader.load(localizedIntKey(subregion.id, "id", dfe))
    }

    /**
     * Lists timezones using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated timezone result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listTimezones(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<TimezonePage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(TimezonePage(
            items = timezoneRepository
                .getTimezonesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(timezoneRepository.countTimezones(), request),
        ), language)
    }

    /**
     * Resolves timezones.
     * @param ids Timezone ids to load. At most 50 ids are allowed.
     * @return Timezones mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun timezones(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<Timezone>> {
        val language = acceptLanguageCode()

        return localizedResult(timezoneRepository
            .getTimezonesByIds(
                ids = longIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single timezone by id.
     * @param id Timezone id to load.
     * @return Matching timezone, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun timezone(
        @InputArgument id: String,
    ): DataFetcherResult<Timezone> {
        val language = acceptLanguageCode()

        return localizedResult(timezoneRepository
            .getTimezoneById(id = longId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Lists central banks using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated central bank result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listCentralBanks(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<CentralBankPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(CentralBankPage(
            items = centralBankRepository
                .getCentralBanksPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(centralBankRepository.countCentralBanks(), request),
        ), language)
    }

    /**
     * Resolves central banks.
     * @param ids Central bank ids to load. At most 50 ids are allowed.
     * @return Central banks mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun centralBanks(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<CentralBank>> {
        val language = acceptLanguageCode()

        return localizedResult(centralBankRepository
            .getCentralBanksByIds(
                ids = intIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single central bank by id.
     * @param id Central bank id to load.
     * @return Matching central bank, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun centralBank(
        @InputArgument id: String,
    ): DataFetcherResult<CentralBank> {
        val language = acceptLanguageCode()

        return localizedResult(centralBankRepository
            .getCentralBankById(
                id = intId(id, "id"),
                languageCode = language
            )
            ?.toGql(), language)
    }

    /**
     * Lists states or provinces using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated state result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listStates(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<StatePage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(StatePage(
            items = stateRepository
                .getStatesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(stateRepository.countStates(), request),
        ), language)
    }

    /**
     * Resolves states or provinces.
     * @param ids State ids to load. At most 50 ids are allowed.
     * @return States mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun states(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<State>> {
        val language = acceptLanguageCode()

        return localizedResult(stateRepository
            .getStatesByIds(
                ids = longIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single state or province by id.
     * @param id State id to load.
     * @return Matching state, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun state(
        @InputArgument id: String,
    ): DataFetcherResult<State> {
        val language = acceptLanguageCode()

        return localizedResult(stateRepository
            .getStateById(id = longId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Lists cities using zero-based pagination.
     * @param page Optional pagination request.
     * Uses the Accept-Language request header to select translated fields.
     * @return Paginated city result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listCities(
        @InputArgument page: PageInput?,
    ): DataFetcherResult<CityPage> {
        val request = pageRequest(page)
        val language = acceptLanguageCode()

        return localizedResult(CityPage(
            items = cityRepository
                .getCitiesPage(language, request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(cityRepository.countCities(), request),
        ), language)
    }

    /**
     * Resolves cities.
     * @param ids City ids to load. At most 50 ids are allowed.
     * @return Cities mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun cities(
        @InputArgument ids: List<String>,
    ): DataFetcherResult<List<City>> {
        val language = acceptLanguageCode()

        return localizedResult(cityRepository
            .getCitiesByIds(
                ids = longIds(ids, "ids"),
                languageCode = language
            )
            .map { it.toGql() }, language)
    }

    /**
     * Resolves a single city by id.
     * @param id City id to load.
     * @return Matching city, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun city(
        @InputArgument id: String,
    ): DataFetcherResult<City> {
        val language = acceptLanguageCode()

        return localizedResult(cityRepository
            .getCityById(id = longId(id, "id"), languageCode = language)
            ?.toGql(), language)
    }

    /**
     * Resolves media assets by id.
     * @param ids Media asset ids to load. At most 50 ids are allowed.
     * @return Media assets mapped to GraphQL DTOs.
     */
    @DgsQuery
    @Authenticated
    suspend fun mediaAssets(@InputArgument ids: List<String>): List<MediaAsset> {
        return mediaAssetRepository.getMediaAssetsByIds(ids = longIds(ids, "ids"))
            .map { it.toGql() }
    }

    /**
     * Resolves a single media asset by id.
     * @param id Media asset id to load.
     * @return Matching media asset, or null when no row exists.
     */
    @DgsQuery
    @Authenticated
    suspend fun mediaAsset(@InputArgument id: String): MediaAsset? {
        return mediaAssetRepository.getMediaAssetById(id = longId(id, "id"))?.toGql()
    }

    /**
     * Lists media assets using zero-based pagination.
     * @param page Optional pagination request.
     * @return Paginated media asset result.
     */
    @DgsQuery
    @Authenticated
    suspend fun listMediaAssets(@InputArgument page: PageInput?): MediaAssetPage {
        val request = pageRequest(page)

        return MediaAssetPage(
            items = mediaAssetRepository.getMediaAssetsPage(request.offset, request.size)
                .map { it.toGql() },
            pageInfo = pageInfo(mediaAssetRepository.countMediaAssets(), request),
        )
    }

    /**
     * Checks whether a currency id exists.
     * @param id Currency id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidCurrency(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = currencyRepository.hasCurrency(id = intId(id, "id")))
    }

    /**
     * Checks whether a country id exists.
     * @param id Country id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidCountry(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = countryRepository.hasCountry(id = intId(id, "id")))
    }

    /**
     * Checks whether a language id exists.
     * @param id Language id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidLanguage(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = languageRepository.hasLanguage(id = intId(id, "id")))
    }

    /**
     * Checks whether a region id exists.
     * @param id Region id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidRegion(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = regionRepository.hasRegion(id = intId(id, "id")))
    }

    /**
     * Checks whether a subregion id exists.
     * @param id Subregion id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidSubregion(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = regionRepository.hasSubregion(id = intId(id, "id")))
    }

    /**
     * Checks whether a timezone id exists.
     * @param id Timezone id to validate.
     * @return Validation response with the existence result.
     */
    @DgsQuery
    @Authenticated
    suspend fun isValidTimezone(@InputArgument id: String): ValidationResponse {
        return ValidationResponse(valid = timezoneRepository.hasTimezone(id = longId(id, "id")))
    }

}
