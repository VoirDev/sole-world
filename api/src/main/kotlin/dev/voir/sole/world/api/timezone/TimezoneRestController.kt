package dev.voir.sole.world.api.timezone

import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.TimezonesApi
import dev.voir.sole.world.openapi.model.Timezone
import dev.voir.sole.world.openapi.model.TimezonePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
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
        val language = RestRequest.language(lang, request)

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

    override fun getTimezone(id: Long, lang: String?): ResponseEntity<Timezone> {
        val language = RestRequest.language(lang, request)
        val timezone =
            timezoneStore.byId(id, language) ?: throw ResourceNotFoundException.of("Timezone", id)

        return cache.ok(timezone.toRest(), request)
    }
}
