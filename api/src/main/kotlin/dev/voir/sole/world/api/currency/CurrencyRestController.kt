package dev.voir.sole.world.api.currency

import dev.voir.sole.world.api.centralbank.CentralBankRestAssembler
import dev.voir.sole.world.api.centralbank.CentralBankStore
import dev.voir.sole.world.api.country.CountryRestAssembler
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.model.CurrencyData
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.CurrenciesApi
import dev.voir.sole.world.openapi.model.CentralBankPage
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.Currency
import dev.voir.sole.world.openapi.model.CurrencyPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for currencies and the records that hang off a currency. */
@RestController
class CurrencyRestController(
    private val currencyStore: CurrencyStore,
    private val countryStore: CountryStore,
    private val centralBankStore: CentralBankStore,
    private val currencies: CurrencyRestAssembler,
    private val countries: CountryRestAssembler,
    private val centralBanks: CentralBankRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : CurrenciesApi {
    override fun listCurrencies(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        obsolete: Boolean?,
    ): ResponseEntity<CurrencyPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CurrencyRestAssembler.ALLOWED_INCLUDES)

        val result = currencyStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
            obsolete = obsolete,
        )

        return cache.ok(
            CurrencyPage(
                items = result.items.map { currencies.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getCurrency(
        idOrCode: String,
        withObsolete: Boolean?,
        lang: String?,
        include: String?,
    ): ResponseEntity<Currency> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CurrencyRestAssembler.ALLOWED_INCLUDES)
        val currency = requireCurrency(idOrCode, withObsolete, language)

        return cache.ok(currencies.assemble(currency, includes, language), request)
    }

    override fun listCurrencyCountries(
        idOrCode: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        // Obsolete currencies still have countries worth listing, so they resolve here.
        val currency = requireCurrency(idOrCode, withObsolete = true, languageCode = language)

        val result = countryStore
            .byCurrencyId(currency.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCurrencyCentralBanks(
        idOrCode: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CentralBankPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CentralBankRestAssembler.ALLOWED_INCLUDES)
        val currency = requireCurrency(idOrCode, withObsolete = true, languageCode = language)

        val result = centralBankStore
            .byCurrencyId(currency.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CentralBankPage(
                items = result.items.map { centralBanks.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    private fun requireCurrency(
        idOrCode: String,
        withObsolete: Boolean?,
        languageCode: String?,
    ): CurrencyData = currencyStore.resolve(idOrCode, withObsolete, languageCode)
        ?: throw ResourceNotFoundException.of("Currency", idOrCode)
}
