package dev.voir.sole.world.api.graphql

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import dev.voir.sole.world.api.graphql.dataLoader.LocalizedIntKey
import dev.voir.sole.world.graphql.dto.types.PageInfo
import dev.voir.sole.world.graphql.dto.types.PageInput
import graphql.execution.DataFetcherResult
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/** Shared validation and context helpers for GraphQL request resolvers. */
object GraphqlRequestUtils {
    private const val DEFAULT_PAGE = 0
    private const val MAX_IDS_PER_QUERY = 50
    private const val MAX_PAGE_SIZE = 50
    private const val MAX_SEARCH_QUERY_LENGTH = 100
    private const val ACCEPT_LANGUAGE_HEADER = "Accept-Language"
    private val supportedLanguageCodes = mapOf(
        "ko" to "ko",
        "pt-br" to "pt-BR",
        "pt" to "pt",
        "nl" to "nl",
        "hr" to "hr",
        "fa" to "fa",
        "de" to "de",
        "es" to "es",
        "fr" to "fr",
        "ja" to "ja",
        "it" to "it",
        "zh-cn" to "zh-CN",
        "tr" to "tr",
        "ru" to "ru",
        "uk" to "uk",
        "pl" to "pl",
    )

    /**
     * Validates the maximum number of requested ids.
     * @param ids Raw GraphQL id values supplied by the caller.
     * @param argumentName GraphQL argument name used in validation error messages.
     * @return The original id list when it is within the configured limit.
     */
    fun limitedIds(ids: List<String>, argumentName: String): List<String> {
        require(ids.size <= MAX_IDS_PER_QUERY) {
            "$argumentName cannot contain more than $MAX_IDS_PER_QUERY values"
        }

        return ids
    }

    /**
     * Parses a bounded list of GraphQL ids into integers.
     * @param ids Raw GraphQL id values supplied by the caller.
     * @param argumentName GraphQL argument name used in validation error messages.
     * @return Parsed integer ids.
     */
    fun intIds(ids: List<String>, argumentName: String): List<Int> {
        return limitedIds(ids, argumentName).map { intId(it, argumentName) }
    }

    /**
     * Parses a bounded list of GraphQL ids into long integers.
     * @param ids Raw GraphQL id values supplied by the caller.
     * @param argumentName GraphQL argument name used in validation error messages.
     * @return Parsed long ids.
     */
    fun longIds(ids: List<String>, argumentName: String): List<Long> {
        return limitedIds(ids, argumentName).map { longId(it, argumentName) }
    }

    /**
     * Parses a single GraphQL id into an integer.
     * @param id Raw GraphQL id value supplied by the caller.
     * @param argumentName GraphQL argument name used in validation error messages.
     * @return Parsed integer id.
     */
    fun intId(id: String, argumentName: String): Int {
        return id.toIntOrNull()
            ?: throw IllegalArgumentException("$argumentName must be a numeric ID")
    }

    /**
     * Parses a single GraphQL id into a long integer.
     * @param id Raw GraphQL id value supplied by the caller.
     * @param argumentName GraphQL argument name used in validation error messages.
     * @return Parsed long id.
     */
    fun longId(id: String, argumentName: String): Long {
        return id.toLongOrNull()
            ?: throw IllegalArgumentException("$argumentName must be a numeric ID")
    }

    /**
     * Builds a DataLoader key that includes the current localized GraphQL context.
     * @param id Raw GraphQL id value supplied by the source object.
     * @param argumentName GraphQL argument or source-field name used in validation error messages.
     * @param dfe Data fetching environment that carries the local language context.
     * @return Localized DataLoader key for batched relationship loading.
     */
    fun localizedIntKey(
        id: String,
        argumentName: String,
        dfe: DgsDataFetchingEnvironment,
    ): LocalizedIntKey {
        return LocalizedIntKey(
            id = intId(id, argumentName),
            languageCode = dfe.getLocalContext<String>(),
        )
    }

    /**
     * Wraps resolver data with a local language context for nested field resolvers.
     * @param data Resolver payload to return to GraphQL.
     * @param languageCode Cleaned language code selected by the current top-level resolver.
     * @return DataFetcherResult containing data plus local context for child resolvers.
     */
    fun <T : Any> localizedResult(data: T?, languageCode: String?): DataFetcherResult<T> {
        return DataFetcherResult.newResult<T>()
            .data(data)
            .localContext(languageCode)
            .build()
    }

    /**
     * Resolves and validates a caller-supplied result limit.
     * @param limit Optional GraphQL limit argument.
     * @return Validated result limit, defaulting to the maximum page size.
     */
    fun limitedResultCount(limit: Int?): Int {
        val resolvedLimit = limit ?: MAX_PAGE_SIZE
        require(resolvedLimit in 1..MAX_PAGE_SIZE) {
            "limit must be between 1 and $MAX_PAGE_SIZE"
        }

        return resolvedLimit
    }

    /**
     * Normalizes an optional short search query.
     * @param query Raw caller-supplied search query.
     * @return Trimmed query, or null when the caller supplied no meaningful filter.
     */
    fun optionalSearchQuery(query: String?): String? {
        val cleanQuery = query?.trim()?.ifBlank { null } ?: return null
        require(cleanQuery.length <= MAX_SEARCH_QUERY_LENGTH) {
            "query cannot be longer than $MAX_SEARCH_QUERY_LENGTH characters"
        }

        return cleanQuery
    }

    /**
     * Resolves and validates GraphQL pagination input.
     * @param pageInput Optional GraphQL pagination input.
     * @return Normalized page request with calculated row offset.
     */
    fun pageRequest(pageInput: PageInput?): PageRequest {
        val page = pageInput?.page ?: DEFAULT_PAGE
        val size = pageInput?.size ?: MAX_PAGE_SIZE

        require(page >= 0) { "page must be greater than or equal to 0" }
        require(size in 1..MAX_PAGE_SIZE) {
            "size must be between 1 and $MAX_PAGE_SIZE"
        }

        return PageRequest(
            page = page,
            size = size,
            offset = page.toLong() * size,
        )
    }

    /**
     * Builds GraphQL pagination metadata for a page response.
     * @param totalItems Total number of rows available across all pages.
     * @param request Normalized page request used to fetch the current page.
     * @return GraphQL page information for the current response.
     */
    fun pageInfo(totalItems: Long, request: PageRequest): PageInfo {
        require(totalItems <= Int.MAX_VALUE) {
            "totalItems exceeds GraphQL Int range"
        }

        val totalPages = if (totalItems == 0L) {
            0
        } else {
            (((totalItems - 1) / request.size) + 1).toInt()
        }

        return PageInfo(
            page = request.page,
            size = request.size,
            totalItems = totalItems.toInt(),
            totalPages = totalPages,
            hasNextPage = request.page + 1 < totalPages,
            hasPreviousPage = request.page > 0 && totalPages > 0,
        )
    }

    /**
     * Resolves the internal translation language from the HTTP Accept-Language header.
     * @return Supported internal language code, or null when English/base data should be used.
     */
    fun acceptLanguageCode(): String? {
        val header = currentAcceptLanguageHeader() ?: return null
        return resolveAcceptLanguage(header)
    }

    /**
     * Maps an Accept-Language header value to a supported internal translation code.
     * @param header Raw Accept-Language header value from the HTTP request.
     * @return Supported internal language code, or null when no supported non-English language matches.
     */
    fun resolveAcceptLanguage(header: String): String? {
        val ranges = header
            .split(',')
            .mapIndexedNotNull(::parseLanguageRange)
            .sortedWith(compareByDescending<LanguageRange> { it.quality }.thenBy { it.order })

        for (range in ranges) {
            if (isEnglishOrWildcard(range.tag)) {
                return null
            }

            val supported = resolveSupportedLanguage(range.tag)
            if (supported != null) {
                return supported
            }
        }

        return null
    }

    /**
     * Reads the current request Accept-Language header.
     * @return Raw Accept-Language header value, or null when no HTTP request is bound.
     */
    private fun currentAcceptLanguageHeader(): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes
            ?.request
            ?.getHeader(ACCEPT_LANGUAGE_HEADER)
            ?.trim()
            ?.ifBlank { null }
    }

    /**
     * Parses one comma-separated Accept-Language range.
     * @param index Original range position used as a stable tie-breaker.
     * @param value Raw language range, optionally with q quality parameter.
     * @return Parsed language range, or null when the range is blank or has q=0.
     */
    private fun parseLanguageRange(index: Int, value: String): LanguageRange? {
        val parts = value.split(';').map { it.trim() }
        val tag = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val quality = parts
            .drop(1)
            .firstNotNullOfOrNull { parameter ->
                parameter
                    .substringAfter("q=", missingDelimiterValue = "")
                    .takeIf { it.isNotBlank() }
                    ?.toDoubleOrNull()
            } ?: 1.0

        if (quality <= 0.0) {
            return null
        }

        return LanguageRange(tag = tag, quality = quality, order = index)
    }

    /**
     * Resolves a language tag to a supported internal language code.
     * @param tag Language tag from Accept-Language, such as "pt-BR" or "de-DE".
     * @return Supported internal language code, or null for English/unsupported tags.
     */
    private fun resolveSupportedLanguage(tag: String): String? {
        val normalized = tag.lowercase()

        return supportedLanguageCodes[normalized]
            ?: supportedLanguageCodes[normalized.substringBefore('-')]
    }

    /**
     * Checks whether a language tag selects English/base data.
     * @param tag Language tag from Accept-Language.
     * @return True when the tag represents English or a wildcard fallback.
     */
    private fun isEnglishOrWildcard(tag: String): Boolean {
        val normalized = tag.lowercase()
        return normalized == "*" || normalized == "en" || normalized.startsWith("en-")
    }

}

private data class LanguageRange(
    val tag: String,
    val quality: Double,
    val order: Int,
)

/**
 * Normalized pagination request used by repository calls.
 * @property page Zero-based page number requested by the caller.
 * @property size Number of items requested for the page.
 * @property offset Zero-based database row offset derived from page and size.
 */
data class PageRequest(
    val page: Int,
    val size: Int,
    val offset: Long,
)
