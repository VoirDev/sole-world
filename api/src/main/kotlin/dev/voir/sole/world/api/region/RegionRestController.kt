package dev.voir.sole.world.api.region

import dev.voir.sole.world.api.country.CountryRestAssembler
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.api.subregion.SubregionRestAssembler
import dev.voir.sole.world.api.subregion.SubregionStore
import dev.voir.sole.world.openapi.api.RegionsApi
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.Region
import dev.voir.sole.world.openapi.model.RegionPage
import dev.voir.sole.world.openapi.model.SubregionPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for regions and the records that hang off a region. */
@RestController
class RegionRestController(
    private val regionStore: RegionStore,
    private val subregionStore: SubregionStore,
    private val countryStore: CountryStore,
    private val regions: RegionRestAssembler,
    private val subregions: SubregionRestAssembler,
    private val countries: CountryRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : RegionsApi {
    override fun listRegions(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<RegionPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, RegionRestAssembler.ALLOWED_INCLUDES)

        val result = regionStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            RegionPage(
                items = result.items.map { regions.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getRegion(id: String, lang: String?, include: String?): ResponseEntity<Region> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, RegionRestAssembler.ALLOWED_INCLUDES)
        val region = regionStore.byId(id, language) ?: throw ResourceNotFoundException.of("Region", id)

        return cache.ok(regions.assemble(region, includes, language), request)
    }

    override fun listRegionSubregions(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<SubregionPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, SubregionRestAssembler.ALLOWED_INCLUDES)
        requireRegion(id, language)

        val result = subregionStore
            .byRegionId(id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            SubregionPage(
                items = result.items.map { subregions.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun listRegionCountries(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        requireRegion(id, language)

        val result = countryStore
            .byRegionId(id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    private fun requireRegion(id: String, languageCode: String?) {
        regionStore.byId(id, languageCode) ?: throw ResourceNotFoundException.of("Region", id)
    }
}
