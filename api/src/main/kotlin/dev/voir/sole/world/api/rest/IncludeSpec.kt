package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.dataset.index.Page

/**
 * The relationships a caller asked to embed in a response.
 *
 * Includes are validated against an explicit per-endpoint allowlist rather than silently ignored.
 * A typo in `include` is far more likely than a deliberate unknown value, and silently dropping it
 * leaves the caller staring at a response that is missing data for no visible reason.
 */
class IncludeSpec private constructor(
    private val requested: Set<String>,
) {
    /** Whether the caller asked for this relationship. */
    operator fun contains(name: String): Boolean = name in requested

    /** Whether the caller asked for nothing at all. */
    fun isEmpty(): Boolean = requested.isEmpty()

    companion object {
        /** Largest number of records returned inside an included collection. */
        const val MAX_INCLUDED_ITEMS = 50

        /** An empty selection, for endpoints that accept no includes. */
        val NONE = IncludeSpec(emptySet())

        /**
         * Parses and validates the `include` query parameter.
         * @param raw Comma-separated relationship names, or null when the caller supplied none.
         * @param allowed Relationship names this endpoint accepts.
         * @return Validated selection.
         * @throws UnknownIncludeException When a requested name is not in [allowed].
         */
        fun parse(raw: String?, allowed: Set<String>): IncludeSpec {
            val names = raw
                ?.split(',')
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?: return NONE

            if (names.isEmpty()) {
                return NONE
            }

            val unknown = names.filterNot { it in allowed }
            if (unknown.isNotEmpty()) {
                throw UnknownIncludeException(unknown = unknown, allowed = allowed)
            }

            return IncludeSpec(names.toSet())
        }
    }
}

/**
 * Raised when `include` names a relationship an endpoint does not offer.
 * @property unknown Names the caller supplied that this endpoint does not accept.
 * @property allowed Names this endpoint does accept, returned so the caller can correct the request.
 */
class UnknownIncludeException(
    val unknown: List<String>,
    val allowed: Set<String>,
) : RuntimeException(
    "Unknown include ${unknown.joinToString(", ")}. " +
        "This endpoint accepts: ${allowed.sorted().joinToString(", ")}.",
)

/**
 * Assembles the included relationships for one response.
 *
 * Collections are capped so that a single `include` can never turn into an unbounded response. When
 * a collection is cut, its name is recorded and surfaced as `truncatedIncludes`, which tells the
 * caller both that there is more and which dedicated endpoint will page through it.
 *
 * @param spec The relationships the caller asked for.
 */
class IncludeAssembler(
    private val spec: IncludeSpec,
) {
    private val truncated = mutableListOf<String>()

    /**
     * Resolves a single related record, when requested.
     * @param name Relationship name.
     * @param load Loads the related record.
     * @return Mapped record, or null when it was not requested or does not exist.
     */
    fun <T : Any, R> one(name: String, load: () -> T?, map: (T) -> R): R? {
        if (name !in spec) {
            return null
        }

        return load()?.let(map)
    }

    /**
     * Resolves a related collection, capped at [IncludeSpec.MAX_INCLUDED_ITEMS], when requested.
     * @param name Relationship name.
     * @param load Loads the related records.
     * @return Mapped records, or null when the relationship was not requested.
     */
    fun <T : Any, R> many(name: String, load: () -> List<T>, map: (T) -> R): List<R>? {
        if (name !in spec) {
            return null
        }

        val all = load()
        if (all.size <= IncludeSpec.MAX_INCLUDED_ITEMS) {
            return all.map(map)
        }

        truncated += name
        return all.take(IncludeSpec.MAX_INCLUDED_ITEMS).map(map)
    }

    /**
     * Resolves a related collection the store has already capped, when requested.
     *
     * Used where loading the whole relationship to cap it afterwards would be wasteful — a country's
     * states, or a state's cities. The store returns one page plus the true total, and the total is
     * what tells us the collection was cut.
     *
     * @param name Relationship name.
     * @param load Loads a page of at most [IncludeSpec.MAX_INCLUDED_ITEMS] records.
     * @return Mapped records, or null when the relationship was not requested.
     */
    fun <T : Any, R> manyPaged(name: String, load: () -> Page<T>, map: (T) -> R): List<R>? {
        if (name !in spec) {
            return null
        }

        val page = load()
        if (page.metadata.totalItems > page.items.size) {
            truncated += name
        }

        return page.items.map(map)
    }

    /** Included collections that were capped, or null when nothing was cut. */
    fun truncatedIncludes(): List<String>? = truncated.ifEmpty { null }
}
