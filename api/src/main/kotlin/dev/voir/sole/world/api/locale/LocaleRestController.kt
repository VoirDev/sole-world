package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.LocalesApi
import dev.voir.sole.world.openapi.model.Locale
import dev.voir.sole.world.openapi.model.LocalePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for the translation locales this deployment serves. */
@RestController
class LocaleRestController(
    private val localeStore: LocaleStore,
    private val locales: LocaleRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : LocalesApi {
    override fun listLocales(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<LocalePage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, LocaleRestAssembler.ALLOWED_INCLUDES)

        val result = localeStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            LocalePage(
                items = result.items.map { locales.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getLocale(id: String, lang: String?, include: String?): ResponseEntity<Locale> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, LocaleRestAssembler.ALLOWED_INCLUDES)
        val locale = localeStore.byId(id, language) ?: throw ResourceNotFoundException.of("Locale", id)

        return cache.ok(locales.assemble(locale, includes, language), request)
    }
}
