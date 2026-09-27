package dev.voir.sole.world.api.language

import dev.voir.sole.world.api.country.CountryRestAssembler
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import dev.voir.sole.world.api.model.LanguageData
import dev.voir.sole.world.api.rest.IncludeSpec
import dev.voir.sole.world.api.rest.ResourceNotFoundException
import dev.voir.sole.world.api.rest.ResponseCache
import dev.voir.sole.world.api.rest.RestRequest
import dev.voir.sole.world.openapi.api.LanguagesApi
import dev.voir.sole.world.openapi.model.CountryPage
import dev.voir.sole.world.openapi.model.Language
import dev.voir.sole.world.openapi.model.LanguagePage
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** REST endpoints for languages and the countries that use them. */
@RestController
class LanguageRestController(
    private val languageStore: LanguageStore,
    private val countryStore: CountryStore,
    private val languages: LanguageRestAssembler,
    private val countries: CountryRestAssembler,
    private val cache: ResponseCache,
    private val request: HttpServletRequest,
) : LanguagesApi {
    override fun listLanguages(
        page: Int,
        size: Int,
        query: String?,
        lang: String?,
        include: String?,
    ): ResponseEntity<LanguagePage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, LanguageRestAssembler.ALLOWED_INCLUDES)

        val result = languageStore.page(
            request = RestRequest.pageRequest(page, size),
            languageCode = language,
            query = RestRequest.searchQuery(query),
        )

        return cache.ok(
            LanguagePage(
                items = result.items.map { languages.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    override fun getLanguage(id: String, lang: String?, include: String?): ResponseEntity<Language> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, LanguageRestAssembler.ALLOWED_INCLUDES)
        val record = requireLanguage(id, language)

        return cache.ok(languages.assemble(record, includes, language), request)
    }

    override fun listLanguageCountries(
        id: String,
        page: Int,
        size: Int,
        lang: String?,
        include: String?,
    ): ResponseEntity<CountryPage> {
        val language = RestRequest.language(request)
        val includes = IncludeSpec.parse(include, CountryRestAssembler.ALLOWED_INCLUDES)
        val record = requireLanguage(id, language)

        val result = countryStore
            .byLanguageId(record.id, language)
            .toPage(RestRequest.pageRequest(page, size))

        return cache.ok(
            CountryPage(
                items = result.items.map { countries.assemble(it, includes, language) },
                page = RestRequest.pageInfo(result.metadata),
            ),
            request,
        )
    }

    private fun requireLanguage(id: String, languageCode: String?): LanguageData =
        languageStore.byId(id, languageCode)
            ?: throw ResourceNotFoundException.of("Language", id)
}
