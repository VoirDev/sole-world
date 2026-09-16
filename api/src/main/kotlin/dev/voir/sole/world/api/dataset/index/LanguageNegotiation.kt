package dev.voir.sole.world.api.dataset.index

/**
 * Maps an HTTP `Accept-Language` header to one of the dataset's translation languages.
 *
 * English and unsupported languages both resolve to null, which selects the base dataset values.
 * Resolution is transport-neutral so the GraphQL and REST layers negotiate identically.
 */
object LanguageNegotiation {
    /** Internal translation codes available in the dataset, keyed by their lowercased tag. */
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

    /** Translation codes this API can serve, in their canonical form. */
    val supportedCodes: List<String> = supportedLanguageCodes.values.distinct().sorted()

    /**
     * Resolves an explicit language selection, such as a REST `lang` query parameter.
     * @param tag Raw language tag supplied by the caller.
     * @return Supported internal language code, or null to serve base data.
     */
    fun resolveTag(tag: String?): String? {
        val cleanTag = tag?.trim()?.ifBlank { null } ?: return null
        return resolveSupportedLanguage(cleanTag)
    }

    /**
     * Maps an `Accept-Language` header value to a supported internal translation code.
     *
     * Ranges are considered in quality order, and the first one that resolves wins. A range that
     * asks for English or the wildcard stops the search and selects base data, so a client asking
     * for `en, ru;q=0.5` is not silently served Russian.
     *
     * @param header Raw header value, or null when the request carries none.
     * @return Supported internal language code, or null to serve base data.
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

            val supported = resolveSupportedLanguage(range.tag)
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
     * Resolves a language tag to a supported internal language code.
     * @param tag Language tag such as "pt-BR" or "de-DE".
     * @return Supported internal code, falling back to the primary subtag, or null when unsupported.
     */
    private fun resolveSupportedLanguage(tag: String): String? {
        val normalized = tag.lowercase()

        return supportedLanguageCodes[normalized]
            ?: supportedLanguageCodes[normalized.substringBefore('-')]
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
