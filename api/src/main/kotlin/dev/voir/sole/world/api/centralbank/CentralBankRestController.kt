package dev.voir.sole.world.api.centralbank

import dev.voir.sole.world.api.country.CountryRestAssembler
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.currency.CurrencyRestAssembler
import dev.voir.sole.world.api.currency.CurrencyStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.CentralBanksApi
import dev.voir.sole.world.openapi.model.CentralBank
import dev.voir.sole.world.openapi.model.CentralBankPage
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.CurrencyPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for central banks and the records that hang off one. */
@RestController
class CentralBankRestController(
    private val centralBankStore: CentralBankStore,
    private val countryStore: CountryStore,
    private val currencyStore: CurrencyStore,
    private val centralBanks: CentralBankRestAssembler,
    private val countries: CountryRestAssembler,
    private val currencies: CurrencyRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : CentralBanksApi {
    override fun listCentralBanks(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<CentralBankPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CentralBankRestAssembler.ALLOWED_INCLUDES)

        val result = centralBankStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            CentralBankPage(
                items = result.items.map { centralBanks.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getCentralBank(id: String, lang: String?, include: String?): ResponseEntity<CentralBank> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CentralBankRestAssembler.ALLOWED_INCLUDES)
        val bank = requireCentralBank(id, language)

        return cache.ok(centralBanks.assemble(bank, includes, language), request)
    }

    override fun listCentralBankCountries(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        requireCentralBank(id, language)

        val result = countryStore
            .byCentralBankId(id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listCentralBankCurrencies(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CurrencyPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CurrencyRestAssembler.ALLOWED_INCLUDES)
        requireCentralBank(id, language)

        val result = currencyStore
            .byCentralBankId(id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CurrencyPage(
                items = result.items.map { currencies.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    private fun requireCentralBank(id: String, languageCode: String?) =
        centralBankStore.byId(id, languageCode) ?: throw ResourceNotFoundException.of("Central bank", id)
}
