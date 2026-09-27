package dev.voir.sole.world.api.dataset.index

/** Orders records by how well they match a search query. */
object Ranking {
    /**
     * Ranks records by relevance, dropping anything that does not match.
     *
     * Each record produces exactly one score, so deduplication is implicit and a caller's limit or
     * page size always applies to distinct records. The SQL implementation this replaces applied its
     * limit before collapsing duplicate join rows, which could silently return fewer records than
     * asked for.
     *
     * @param records Candidate records.
     * @param query Prepared search query.
     * @param score Relevance of one record against the query; zero or less means no match.
     * @param tieBreaker Stable value, usually the identifier, ordering records of equal relevance.
     * @return Matching records, most relevant first.
     */
    fun <T> rank(
        records: List<T>,
        query: SearchQuery,
        score: (T, SearchQuery) -> Int,
        tieBreaker: (T) -> Comparable<*>,
    ): List<T> {
        val scored = ArrayList<Pair<T, Int>>()
        for (record in records) {
            val relevance = score(record, query)
            if (relevance > 0) {
                scored += record to relevance
            }
        }

        scored.sortWith(compareByDescending<Pair<T, Int>> { it.second }.thenBy { tieBreaker(it.first) })
        return scored.map { it.first }
    }
}
