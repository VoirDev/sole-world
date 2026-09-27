package dev.voir.sole.world.api.language

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.country.CountryStore
import dev.voir.sole.world.api.country.toGql
import dev.voir.sole.world.api.flag.FlagStore
import dev.voir.sole.world.api.flag.toGql
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.graphql.dto.types.Country
import dev.voir.sole.world.graphql.dto.types.Flag
import dev.voir.sole.world.graphql.dto.types.Language
import dev.voir.sole.world.graphql.dto.types.LanguagePage
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for languages and every field hanging off a language. */
@DgsComponent
class LanguageGraphqlFetcher(
    private val languageStore: LanguageStore,
    private val countryStore: CountryStore,
    private val flagStore: FlagStore,
) {
    /**
     * Lists or searches languages.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @return Paginated language result.
     */
    @DgsQuery
    fun languages(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): DataFetcherResult<LanguagePage> {
        val language = GraphqlRequest.language()
        val result = languageStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return GraphqlRequest.localized(
            LanguagePage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads languages by id.
     * @param ids Language ids to load; at most 50.
     * @return Matching languages.
     */
    @DgsQuery
    fun languagesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Language>> {
        val language = GraphqlRequest.language()
        val languages = languageStore.byIds(GraphqlRequest.ids(ids, "ids"), language)

        return GraphqlRequest.localized(languages.map { it.toGql() }, language)
    }

    /**
     * Loads one language.
     * @param id ISO 639-1 language code, in any case.
     * @return Matching language, or null when none exists.
     */
    @DgsQuery
    fun language(@InputArgument id: String): DataFetcherResult<Language> {
        val languageCode = GraphqlRequest.language()
        val language = languageStore.byId(id, languageCode)

        return GraphqlRequest.localized(language?.toGql(), languageCode)
    }

    /** Resolves the countries associated with a language. */
    @DgsData(parentType = "Language", field = "countries")
    fun countries(dfe: DgsDataFetchingEnvironment): List<Country>? {
        val source: Language = dfe.getSource() ?: return null
        val languageCode = GraphqlRequest.language(dfe)

        return countryStore
            .byLanguageId(source.id, languageCode)
            .map { it.toGql() }
    }

    /** Resolves the flag associated with a language. */
    @DgsData(parentType = "Language", field = "flag")
    fun flag(dfe: DgsDataFetchingEnvironment): Flag? {
        val source: Language = dfe.getSource() ?: return null
        val flagId = source.flagId ?: return null

        return flagStore.byId(flagId)?.toGql()
    }
}
