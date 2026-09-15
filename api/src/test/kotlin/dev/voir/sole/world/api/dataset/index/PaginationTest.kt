package dev.voir.sole.world.api.dataset.index

import dev.voir.sole.world.api.dataset.index.Pagination.toPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaginationTest {
    @Test
    fun `defaults to the first page at the maximum size`() {
        val request = Pagination.request(page = null, size = null)

        assertEquals(0, request.page)
        assertEquals(Pagination.MAX_PAGE_SIZE, request.size)
        assertEquals(0, request.offset)
    }

    @Test
    fun `offset follows page and size`() {
        assertEquals(60, Pagination.request(page = 3, size = 20).offset)
    }

    @Test
    fun `rejects a negative page`() {
        assertThrows<IllegalArgumentException> { Pagination.request(page = -1, size = 10) }
    }

    @Test
    fun `rejects sizes outside the supported range`() {
        assertThrows<IllegalArgumentException> { Pagination.request(page = 0, size = 0) }
        assertThrows<IllegalArgumentException> {
            Pagination.request(page = 0, size = Pagination.MAX_PAGE_SIZE + 1)
        }
    }

    @Test
    fun `slices a list into pages`() {
        val items = (1..10).toList()

        assertEquals(listOf(1, 2, 3), items.toPage(PageRequest(page = 0, size = 3)).items)
        assertEquals(listOf(4, 5, 6), items.toPage(PageRequest(page = 1, size = 3)).items)
        assertEquals(listOf(10), items.toPage(PageRequest(page = 3, size = 3)).items)
    }

    @Test
    fun `a page past the end is empty rather than an error`() {
        val page = (1..10).toList().toPage(PageRequest(page = 99, size = 10))

        assertTrue(page.items.isEmpty())
        assertEquals(10, page.metadata.totalItems)
        assertFalse(page.metadata.hasNextPage)
        assertTrue(page.metadata.hasPreviousPage)
    }

    @Test
    fun `metadata reports total pages and neighbours`() {
        val metadata = Pagination.metadata(totalItems = 10, request = PageRequest(page = 1, size = 3))

        assertEquals(1, metadata.page)
        assertEquals(3, metadata.size)
        assertEquals(10, metadata.totalItems)
        assertEquals(4, metadata.totalPages)
        assertTrue(metadata.hasNextPage)
        assertTrue(metadata.hasPreviousPage)
    }

    @Test
    fun `an exact multiple does not report a trailing empty page`() {
        val metadata = Pagination.metadata(totalItems = 9, request = PageRequest(page = 2, size = 3))

        assertEquals(3, metadata.totalPages)
        assertFalse(metadata.hasNextPage)
    }

    @Test
    fun `an empty result has no pages and no neighbours`() {
        val metadata = Pagination.metadata(totalItems = 0, request = PageRequest(page = 0, size = 10))

        assertEquals(0, metadata.totalPages)
        assertFalse(metadata.hasNextPage)
        assertFalse(metadata.hasPreviousPage)
    }
}
