package dev.voir.sole.world.api.flag

import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.FlagsApi
import dev.voir.sole.world.openapi.model.Flag
import dev.voir.sole.world.openapi.model.FlagPage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for flags. Captions are not translated, so language only selects collation. */
@RestController
class FlagRestController(
    private val flagStore: FlagStore,
    private val flags: FlagRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : FlagsApi {
    override fun listFlags(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<FlagPage> {
        val includes = IncludeSpec.parse(include, FlagRestAssembler.ALLOWED_INCLUDES)
        val result = flagStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = RestRequest.language(lang, request),
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            FlagPage(
                items = result.items.map { flags.assemble(it, includes) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getFlag(id: Long, include: String?): ResponseEntity<Flag> {
        val includes = IncludeSpec.parse(include, FlagRestAssembler.ALLOWED_INCLUDES)
        val flag = flagStore.byId(id) ?: throw ResourceNotFoundException.of("Flag", id)

        return cache.ok(flags.assemble(flag, includes), request)
    }
}
