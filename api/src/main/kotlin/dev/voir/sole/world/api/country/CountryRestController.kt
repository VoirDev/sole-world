package dev.voir.sole.world.api.country

import dev.voir.sole.world.api.centralbank.CentralBankRestAssembler
import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.city.CityRestAssembler
import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.currency.CurrencyRestAssembler
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.language.LanguageRestAssembler
import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.model.CountryData
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.api.state.StateRestAssembler
import dev.voir.sole.world.api.state.StateStore
import dev.voir.sole.world.api.timezone.TimezoneStore
import dev.voir.sole.world.api.timezone.toRest
import dev.voir.sole.world.openapi.api.CountriesApi
import dev.voir.sole.world.openapi.model.CentralBankPage
import dev.voir.sole.world.openapi.model.CityPage
import dev.voir.sole.world.openapi.model.Country
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.CurrencyPage
import dev.voir.sole.world.openapi.model.LanguagePage
import dev.voir.sole.world.openapi.model.StatePage
import dev.voir.sole.world.openapi.model.TimezonePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for countries and the records that hang off a country. */
@RestController
class CountryRestController(
    private val countryStore: CountryStore,
    private val stateStore: StateStore,
    private val cityStore: CityStore,
    private val currencyStore: CurrencyStore,
    private val languageStore: LanguageStore,
    private val timezoneStore: TimezoneStore,
    private val centralBankStore: CentralBankStore,
    private val countries: CountryRestAssembler,
    private val states: StateRestAssembler,
    private val cities: CityRestAssembler,
    private val currencies: CurrencyRestAssembler,
    private val languages: LanguageRestAssembler,
    private val centralBanks: CentralBankRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : CountriesApi {
    override fun listCountries(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        regionId: String?,
        subregionId: String?,
        currencyId: String?,
        languageId: String?,
        timezoneId: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)

        val result = countryStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
            regionId = regionId,
            subregionId = subregionId,
            currencyId = currencyId,
            languageId = languageId,
            timezoneId = timezoneId,
        )

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getCountry(id: String, lang: String?, include: String?): ResponseEntity<Country> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        return cache.ok(countries.assemble(country, includes, language), request)
    }

    override fun listCountryStates(
        id: String,
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<StatePage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, StateRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        val result = stateStore.pageByCountryId(
            countryId = country.id,
            request = RestRequest.pageRequest(page, size),
            query = RestRequest.searchQuery(query),
            languageCode = language,
        )

        return cache.ok(
            StatePage(
                items = result.items.map { states.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCountryCities(
        id: String,
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<CityPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CityRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        val result = cityStore.pageByCountryId(
            countryId = country.id,
            request = RestRequest.pageRequest(page, size),
            query = RestRequest.searchQuery(query),
            languageCode = language,
        )

        return cache.ok(
            CityPage(
                items = result.items.map { cities.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCountryCurrencies(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CurrencyPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CurrencyRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        val result = currencyStore
            .byCountryId(country.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CurrencyPage(
                items = result.items.map { currencies.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCountryLanguages(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<LanguagePage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, LanguageRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        val result = languageStore
            .byCountryId(country.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            LanguagePage(
                items = result.items.map { languages.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCountryTimezones(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
    ): ResponseEntity<TimezonePage> {
        val language = RestRequest.language(request)
        val country = requireCountry(id, language)

        val result = timezoneStore
            .byCountryId(country.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            TimezonePage(
                items = result.items.map { it.toRest() },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCountryCentralBanks(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CentralBankPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CentralBankRestAssembler.ALLOWED_INCLUDES)
        val country = requireCountry(id, language)

        val result = centralBankStore
            .byCountryId(country.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CentralBankPage(
                items = result.items.map { centralBanks.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    /**
     * Resolves a country path identifier or fails with a not-found problem.
     *
     * Sub-resource endpoints resolve the parent first so that a bad country identifier is a `404`
     * about the country, not an empty page that looks like the country simply has no records.
     */
    private fun requireCountry(id: String, languageCode: String?): CountryData =
        countryStore.byIdentifier(id, languageCode)
            ?: throw ResourceNotFoundException.of("Country", id)
}
