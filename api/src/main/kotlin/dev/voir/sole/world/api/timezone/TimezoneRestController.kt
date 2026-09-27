package dev.voir.sole.world.api.timezone

import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.TimezonesApi
import dev.voir.sole.world.openapi.model.Timezone
import dev.voir.sole.world.openapi.model.TimezonePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for timezones. */
@RestController
class TimezoneRestController(
    private val timezoneStore: TimezoneStore,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : TimezonesApi {
    override fun listTimezones(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
    ): ResponseEntity<TimezonePage> {
        val language = RestRequest.language(request)

        val result = timezoneStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            TimezonePage(
                items = result.items.map { it.toRest() },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    /**
     * Loads one timezone by its IANA name.
     *
     * The name is the identifier and has slashes in it — `Europe/Paris`, and three segments for
     * `America/Argentina/Buenos_Aires` — so the generated single-segment `{id}` mapping cannot
     * route it. This mapping replaces it with a capture of the rest of the path. An encoded `%2F`
     * would not help instead: the servlet container rejects it before the request reaches Spring.
     *
     * @param id Rest of the path after `/v1/timezones`, captured with its leading slash.
     * @param lang Translation language, overriding `Accept-Language`.
     * @return The timezone, or a not-found problem.
     */
    @GetMapping(
        "/v1/timezones/{*id}",
        produces = ["application/json", "application/problem+json"],
    )
    override fun getTimezone(id: String, lang: String?): ResponseEntity<Timezone> {
        val zoneName = id.removePrefix("/")
        val language = RestRequest.language(request)
        val timezone = timezoneStore.byId(zoneName, language)
            ?: throw ResourceNotFoundException.of("Timezone", zoneName)

        return cache.ok(timezone.toRest(), request)
    }
}
