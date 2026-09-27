package dev.voir.sole.world.api.locale

import dev.voir.sole.world.api.dataset.json.LocaleJSON

/**
 * Maps a caller's language preference to one of the dataset's translation locales.
 *
 * English and unsupported languages both resolve to null, which selects the base dataset values.
 * Resolution is transport-neutral so the GraphQL and REST layers negotiate identically.
 *
 * A tag that names no locale exactly falls back to its language: `de-AT` is served `de`, and `zh-TW`
 * is served `zh-CN`. When a language has several locales, the one spelled as the bare language wins,
 * so `pt-PT` is served `pt` rather than `pt-BR`; failing that, the first one `locales.json` lists.
 *
 * @param locales Translation locales the dataset declares.
 */
class LocaleNegotiation(locales: List<LocaleJSON>) {
    /** Locale ids keyed by their lowercased tag. */
    private val byTag: Map<String, String> = locales.associate { it.id.lowercase() to it.id }

    /** The locale each language falls back to, keyed by its lowercased language id. */
    private val byLanguage: Map<String, String> = locales
        .groupBy { it.languageId.lowercase() }
        .mapValues { (language, forms) ->
            (forms.firstOrNull { it.id.lowercase() == language } ?: forms.first()).id
        }

    /** Locale ids this API can serve, in their canonical spelling. */
    val supportedIds: List<String> = byTag.values.sorted()

    /**
     * Resolves an explicit locale selection, such as a REST `lang` query parameter.
     * @param tag Raw language tag supplied by the caller.
     * @return Supported locale id, or null to serve base data.
     */
    fun resolveTag(tag: String?): String? {
        val cleanTag = tag?.trim()?.ifBlank { null } ?: return null
        return resolveSupportedLocale(cleanTag)
    }

    /**
     * Maps an `Accept-Language` header value to a supported locale.
     *
     * Ranges are considered in quality order, and the first one that resolves wins. A range that
     * asks for English or the wildcard stops the search and selects base data, so a client asking
     * for `en, ru;q=0.5` is not silently served Russian.
     *
     * @param header Raw header value, or null when the request carries none.
     * @return Supported locale id, or null to serve base data.
     */
    fun resolveHeader(header: String?): String? {
        val cleanHeader = header?.trim()?.ifBlank { null } ?: return null

        val ranges = cleanHeader
            .split(',')
            .mapIndexedNotNull(::parseLanguageRange)
            .sortedWith(compareByDescending<LanguageRange> { it.quality }.thenBy { it.order })

        for (range in ranges) {
            if (isEnglishOrWildcard(range.tag)) {
                return null
            }

            val supported = resolveSupportedLocale(range.tag)
            if (supported != null) {
                return supported
            }
        }

        return null
    }

    /**
     * Parses one comma-separated `Accept-Language` range.
     * @param index Original range position, used as a stable tie-breaker between equal qualities.
     * @param value Raw language range, optionally with a q quality parameter.
     * @return Parsed range, or null when the range is blank or explicitly unwanted with q=0.
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
     * Resolves a language tag to a supported locale.
     * @param tag Language tag such as "pt-BR" or "de-DE".
     * @return Locale named exactly, else the locale of the tag's language, or null when unsupported.
     */
    private fun resolveSupportedLocale(tag: String): String? {
        val normalized = tag.lowercase()

        return byTag[normalized] ?: byLanguage[normalized.substringBefore('-')]
    }

    /**
     * Checks whether a language tag selects the base English dataset.
     * @param tag Language tag from the header.
     * @return True for English and for the wildcard fallback.
     */
    private fun isEnglishOrWildcard(tag: String): Boolean {
        val normalized = tag.lowercase()
        return normalized == "*" || normalized == "en" || normalized.startsWith("en-")
    }

    private data class LanguageRange(
        val tag: String,
        val quality: Double,
        val order: Int,
    )
}
