package dev.voir.sole.world.api.dataset.index

/**
 * Normalized pagination request shared by both transports.
 * @property page Zero-based page number.
 * @property size Number of items requested for the page.
 */
data class PageRequest(
    val page: Int,
    val size: Int,
) {
    /** Zero-based index of the first item on this page. */
    val offset: Int get() = page * size
}

/**
 * Pagination metadata describing where a page sits in the full result.
 * @property page Zero-based page number returned.
 * @property size Number of items requested for this page.
 * @property totalItems Total matching items across all pages.
 * @property totalPages Total number of available pages.
 * @property hasNextPage Whether another page exists after this one.
 * @property hasPreviousPage Whether a page exists before this one.
 */
data class PageMetadata(
    val page: Int,
    val size: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
)

/**
 * One page of results together with its metadata.
 * @property items Items on the requested page.
 * @property metadata Where this page sits in the full result.
 */
data class Page<T>(
    val items: List<T>,
    val metadata: PageMetadata,
)

/** Validates page requests and slices already-ordered lists into pages. */
object Pagination {
    /** Largest page a caller may request, on either transport. */
    const val MAX_PAGE_SIZE = 50

    /** Page size used when a caller does not ask for one. */
    const val DEFAULT_PAGE_SIZE = MAX_PAGE_SIZE

    private const val DEFAULT_PAGE = 0

    /**
     * Validates a caller-supplied page request.
     * @param page Zero-based page number, or null for the first page.
     * @param size Page size, or null for the default.
     * @return Normalized page request.
     * @throws IllegalArgumentException When the page or size is outside the supported range.
     */
    fun request(page: Int?, size: Int?): PageRequest {
        val resolvedPage = page ?: DEFAULT_PAGE
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE

        require(resolvedPage >= 0) { "page must be greater than or equal to 0" }
        require(resolvedSize in 1..MAX_PAGE_SIZE) { "size must be between 1 and $MAX_PAGE_SIZE" }

        return PageRequest(page = resolvedPage, size = resolvedSize)
    }

    /**
     * Slices an ordered list into the requested page.
     *
     * A page past the end is empty rather than an error, which keeps callers walking a changing
     * result set from having to special-case the last page.
     *
     * @param request Normalized page request.
     * @return Requested page and its metadata.
     */
    fun <T> List<T>.toPage(request: PageRequest): Page<T> {
        val from = minOf(request.offset, size)
        val to = minOf(from + request.size, size)

        return Page(
            items = subList(from, to).toList(),
            metadata = metadata(totalItems = size, request = request),
        )
    }

    /**
     * Builds pagination metadata for a total that was counted separately from the page.
     * @param totalItems Total matching items across all pages.
     * @param request Normalized page request used to fetch the page.
     * @return Metadata describing the page.
     */
    fun metadata(totalItems: Int, request: PageRequest): PageMetadata {
        val totalPages = if (totalItems == 0) 0 else ((totalItems - 1) / request.size) + 1

        return PageMetadata(
            page = request.page,
            size = request.size,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNextPage = request.page + 1 < totalPages,
            hasPreviousPage = request.page > 0 && totalPages > 0,
        )
    }
}
