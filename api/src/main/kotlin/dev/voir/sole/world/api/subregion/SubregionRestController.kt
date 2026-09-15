package dev.voir.sole.world.api.subregion

import dev.voir.sole.world.api.country.CountryRestAssembler
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.SubregionsApi
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.Subregion
import dev.voir.sole.world.openapi.model.SubregionPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for subregions and the countries inside them. */
@RestController
class SubregionRestController(
    private val subregionStore: SubregionStore,
    private val countryStore: CountryStore,
    private val subregions: SubregionRestAssembler,
    private val countries: CountryRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : SubregionsApi {
    override fun listSubregions(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        regionId: Long?,
    ): ResponseEntity<SubregionPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, SubregionRestAssembler.ALLOWED_INCLUDES)

        val result = subregionStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
            regionId = regionId,
        )

        return cache.ok(
            SubregionPage(
                items = result.items.map { subregions.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getSubregion(id: Long, lang: String?, include: String?): ResponseEntity<Subregion> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, SubregionRestAssembler.ALLOWED_INCLUDES)
        val subregion =
            subregionStore.byId(id, language) ?: throw ResourceNotFoundException.of("Subregion", id)

        return cache.ok(subregions.assemble(subregion, includes, language), request)
    }

    override fun listSubregionCountries(
        id: Long,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        subregionStore.byId(id, language) ?: throw ResourceNotFoundException.of("Subregion", id)

        val result = countryStore
            .bySubregionId(id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }
}
