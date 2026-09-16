package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.dataset.index.SortOrder

/**
 * Parses the `sort` and `order` query parameters.
 *
 * The values are matched against an explicit allowlist and a typo is rejected rather than ignored,
 * for the same reason an unknown `include` is: a caller who mistypes `popularty` would otherwise
 * get a silently different ordering and no hint as to why.
 */
object SortSpec {
    /**
     * Parses a sort field.
     * @param raw Caller-supplied field name, or null to let the endpoint choose.
     * @param entries Fields this endpoint sorts by, in the order they are documented.
     * @param wireName Query-parameter spelling of a field.
     * @return Parsed field, or null when the caller supplied none.
     * @throws UnknownSortException When the value is not a field this endpoint offers.
     */
    fun <T> field(raw: String?, entries: List<T>, wireName: (T) -> String): T? {
        val value = raw.clean() ?: return null

        return entries.firstOrNull { wireName(it).equals(value, ignoreCase = true) }
            ?: throw UnknownSortException(
                parameter = "sort",
                unknown = value,
                allowed = entries.map(wireName),
            )
    }

    /**
     * Parses a sort direction.
     * @param raw Caller-supplied direction, or null to use the field's own default.
     * @return Parsed direction, or null when the caller supplied none.
     * @throws UnknownSortException When the value is not a direction this API offers.
     */
    fun order(raw: String?): SortOrder? {
        val value = raw.clean() ?: return null

        return SortOrder.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw UnknownSortException(
                parameter = "order",
                unknown = value,
                allowed = SortOrder.entries.map { it.name.lowercase() },
            )
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}

/**
 * Raised when `sort` or `order` names something an endpoint does not offer.
 * @property parameter Query parameter that carried the unknown value.
 * @property unknown Value the caller supplied.
 * @property allowed Values this parameter accepts, returned so the caller can correct the request.
 */
class UnknownSortException(
    val parameter: String,
    val unknown: String,
    val allowed: List<String>,
) : RuntimeException(
    "Unknown $parameter '$unknown'. This endpoint accepts: ${allowed.joinToString(", ")}.",
)
