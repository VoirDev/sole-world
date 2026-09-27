package dev.voir.sole.world.api.city

import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.CitiesApi
import dev.voir.sole.world.openapi.model.City
import dev.voir.sole.world.openapi.model.CityPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for cities. */
@RestController
class CityRestController(
    private val cityStore: CityStore,
    private val cities: CityRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : CitiesApi {
    override fun listCities(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        countryId: String?,
        stateId: String?,
    ): ResponseEntity<CityPage> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CityRestAssembler.ALLOWED_INCLUDES)

        val result = cityStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
            countryId = countryId,
            stateId = stateId,
        )

        return cache.ok(
            CityPage(
                items = result.items.map { cities.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getCity(id: Long, lang: String?, include: String?): ResponseEntity<City> {
        val language = RestRequest.language(lang, request)
        val includes = IncludeSpec.parse(include, CityRestAssembler.ALLOWED_INCLUDES)
        val city = cityStore.byId(id, language) ?: throw ResourceNotFoundException.of("City", id)

        return cache.ok(cities.assemble(city, includes, language), request)
    }
}
