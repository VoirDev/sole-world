package dev.voir.sole.world.api.locale

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import dev.voir.sole.world.api.graphql.GraphqlRequest
import dev.voir.sole.world.api.language.LanguageStore
import dev.voir.sole.world.api.language.toGql
import dev.voir.sole.world.graphql.dto.types.Language
import dev.voir.sole.world.graphql.dto.types.Locale
import dev.voir.sole.world.graphql.dto.types.LocalePage
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult

/** GraphQL fetchers for translation locales and every field hanging off a locale. */
@DgsComponent
class LocaleGraphqlFetcher(
    private val localeStore: LocaleStore,
    private val languageStore: LanguageStore,
) {
    /**
     * Lists or searches locales.
     * @param page Optional pagination request.
     * @param query Optional relevance search.
     * @return Paginated locale result.
     */
    @DgsQuery
    fun locales(
        @InputArgument page: PageInput?,
        @InputArgument query: String?,
    ): DataFetcherResult<LocalePage> {
        val language = GraphqlRequest.language()
        val result = localeStore.page(
            request = GraphqlRequest.pageRequest(page),
            languageCode = language,
            query = GraphqlRequest.optionalSearchQuery(query),
        )

        return GraphqlRequest.localized(
            LocalePage(
                items = result.items.map { it.toGql() },
                pageInfo = GraphqlRequest.pageInfo(result.metadata),
            ),
            language,
        )
    }

    /**
     * Loads locales by id.
     * @param ids Locale ids to load; at most 50.
     * @return Matching locales.
     */
    @DgsQuery
    fun localesByIds(@InputArgument ids: List<String>): DataFetcherResult<List<Locale>> {
        val language = GraphqlRequest.language()
        val locales = localeStore.byIds(GraphqlRequest.ids(ids, "ids"), language)

        return GraphqlRequest.localized(locales.map { it.toGql() }, language)
    }

    /**
     * Loads one locale.
     * @param id BCP 47 language tag, in any case.
     * @return Matching locale, or null when none exists.
     */
    @DgsQuery
    fun locale(@InputArgument id: String): DataFetcherResult<Locale> {
        val language = GraphqlRequest.language()
        val locale = localeStore.byId(id, language)

        return GraphqlRequest.localized(locale?.toGql(), language)
    }

    /** Resolves the language a locale is a form of. */
    @DgsData(parentType = "Locale", field = "language")
    fun language(dfe: DgsDataFetchingEnvironment): Language? {
        val source: Locale = dfe.getSource() ?: return null
        val language = GraphqlRequest.language(dfe)

        return languageStore.byId(source.languageId, language)?.toGql()
    }
}
