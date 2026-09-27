package dev.voir.sole.world.api.locale

import jakarta.servlet.http.HttpServletRequest

/**
 * The translation locale negotiated for the current request.
 *
 * [RequestLocaleFilter] negotiates once, before any handler runs, and records the result on the
 * request. Everything downstream — REST controllers, GraphQL fetchers, the response validator — reads
 * it back from here, so they cannot disagree about which language a response is in.
 */
object RequestLocale {
    /** `Content-Language` of a response rendered from the base data. */
    const val BASE = "en"

    private val ATTRIBUTE = RequestLocale::class.java.name

    /**
     * Records the negotiated locale.
     * @param request Current HTTP request.
     * @param localeId Supported locale id, or null to serve base data.
     */
    fun set(request: HttpServletRequest, localeId: String?) {
        request.setAttribute(ATTRIBUTE, localeId)
    }

    /**
     * Reads the negotiated locale.
     * @param request Current HTTP request.
     * @return Supported locale id, or null to serve base data.
     */
    fun of(request: HttpServletRequest): String? = request.getAttribute(ATTRIBUTE) as String?

    /**
     * Names the language a response is written in, for its `Content-Language` header.
     * @param localeId Supported locale id, or null for base data.
     * @return The locale id, or [BASE] when the response is base data.
     */
    fun contentLanguage(localeId: String?): String = localeId ?: BASE
}
