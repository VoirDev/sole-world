package dev.voir.sole.world.api.state

import dev.voir.sole.world.api.city.CityRestAssembler
import dev.voir.sole.world.api.city.CityStore
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.StatesApi
import dev.voir.sole.world.openapi.model.CityPage
import dev.voir.sole.world.openapi.model.State
import dev.voir.sole.world.openapi.model.StatePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for states and the cities inside them. */
@RestController
class StateRestController(
    private val stateStore: StateStore,
    private val cityStore: CityStore,
    private val states: StateRestAssembler,
    private val cities: CityRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : StatesApi {
    override fun listStates(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
        countryId: String?,
    ): ResponseEntity<StatePage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, StateRestAssembler.ALLOWED_INCLUDES)

        val result = stateStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
            countryId = countryId,
        )

        return cache.ok(
            StatePage(
                items = result.items.map { states.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getState(id: String, lang: String?, include: String?): ResponseEntity<State> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, StateRestAssembler.ALLOWED_INCLUDES)
        val state = stateStore.byId(id, language) ?: throw ResourceNotFoundException.of("State", id)

        return cache.ok(states.assemble(state, includes, language), request)
    }

    override fun listStateCities(
        id: String,
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<CityPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CityRestAssembler.ALLOWED_INCLUDES)
        stateStore.byId(id, language) ?: throw ResourceNotFoundException.of("State", id)

        val result = cityStore.pageByStateId(
            stateId = id,
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
}
