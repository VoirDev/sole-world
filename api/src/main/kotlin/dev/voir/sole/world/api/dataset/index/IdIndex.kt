package dev.voir.sole.world.api.dataset.index

/**
 * Values looked up by identifier, matched regardless of case.
 *
 * A plain map keyed by identifier would work too, until one lookup forgot to normalize its key and
 * `/v1/countries/us` quietly stopped finding `US`. Nothing would fail to compile. Holding the rule
 * here means a store cannot index or look up an identifier any other way than [IdKey] does.
 *
 * Identifiers are unique regardless of case — `DatasetIntegrity` fails startup otherwise — so no two
 * values ever compete for one key.
 *
 * @param V Value held per identifier: a record, or the records related to it.
 */
class IdIndex<V> private constructor(private val entries: Map<String, V>) {
    /**
     * Looks up the value held for an identifier.
     * @param id Identifier in any case, as published or as a caller supplied it.
     * @return Value held for that identifier, or null when there is none.
     */
    operator fun get(id: String): V? = entries[IdKey.of(id)]

    /** Builders for the shapes of index the stores need. */
    companion object {
        /**
         * Indexes records by their own identifier.
         * @param records Records to index; their identifiers are unique.
         * @param id Reads a record's identifier.
         * @return Index from identifier to record.
         */
        fun <T> of(records: Iterable<T>, id: (T) -> String): IdIndex<T> =
            IdIndex(records.associateBy { IdKey.of(id(it)) })

        /**
         * Groups records by the identifier of something they belong to, such as a parent region.
         * @param records Records to group, in the order each group should keep.
         * @param id Reads the identifier a record is grouped under.
         * @return Index from identifier to the records grouped under it.
         */
        fun <T> grouped(records: Iterable<T>, id: (T) -> String): IdIndex<List<T>> =
            IdIndex(records.groupBy { IdKey.of(id(it)) })

        /**
         * Wraps entries already keyed by identifier.
         * @param entries Values keyed by identifier in any case.
         * @return Index over the same entries.
         */
        fun <V> from(entries: Map<String, V>): IdIndex<V> =
            IdIndex(entries.mapKeys { (id, _) -> IdKey.of(id) })
    }
}
