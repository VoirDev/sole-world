package dev.voir.sole.world.api.rest

import dev.voir.sole.world.api.dataset.index.Page
import dev.voir.sole.world.api.dataset.index.PageMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IncludeSpecTest {
    private val allowed = setOf("region", "currencies", "states")

    @Test
    fun `no include parameter selects nothing`() {
        assertTrue(IncludeSpec.parse(null, allowed).isEmpty())
        assertTrue(IncludeSpec.parse("", allowed).isEmpty())
        assertTrue(IncludeSpec.parse("  ", allowed).isEmpty())
    }

    @Test
    fun `parses a comma separated list and tolerates spacing`() {
        val spec = IncludeSpec.parse(" region , currencies ", allowed)

        assertTrue("region" in spec)
        assertTrue("currencies" in spec)
        assertFalse("states" in spec)
    }

    @Test
    fun `ignores empty entries between separators`() {
        val spec = IncludeSpec.parse("region,,currencies,", allowed)

        assertTrue("region" in spec)
        assertTrue("currencies" in spec)
    }

    @Test
    fun `an unknown value is rejected and reports what is accepted`() {
        val failure = assertThrows<UnknownIncludeException> {
            IncludeSpec.parse("region,citiez", allowed)
        }

        assertEquals(listOf("citiez"), failure.unknown)
        assertEquals(allowed, failure.allowed)
    }

    @Test
    fun `include names are case sensitive`() {
        assertThrows<UnknownIncludeException> { IncludeSpec.parse("Region", allowed) }
    }

    @Test
    fun `a relationship that was not requested is not loaded at all`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("region", allowed))
        var loaded = false

        val result = assembler.many("currencies", {
            loaded = true
            listOf("EUR")
        }) { it }

        assertNull(result)
        assertFalse(loaded, "an unrequested relationship must not hit the store")
    }

    @Test
    fun `a collection within the cap is returned whole`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("currencies", allowed))
        val values = List(IncludeSpec.MAX_INCLUDED_ITEMS) { "c$it" }

        assertEquals(values, assembler.many("currencies", { values }) { it })
        assertNull(assembler.truncatedIncludes(), "nothing was cut")
    }

    @Test
    fun `a collection over the cap is cut and reported`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("currencies", allowed))
        val values = List(IncludeSpec.MAX_INCLUDED_ITEMS + 1) { "c$it" }

        val included = assembler.many("currencies", { values }) { it }

        assertEquals(IncludeSpec.MAX_INCLUDED_ITEMS, included?.size)
        assertEquals(listOf("currencies"), assembler.truncatedIncludes())
    }

    @Test
    fun `a store-capped collection is reported as cut when the total exceeds the page`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("states", allowed))
        val page = Page(
            items = List(IncludeSpec.MAX_INCLUDED_ITEMS) { "s$it" },
            metadata = PageMetadata(
                page = 0,
                size = IncludeSpec.MAX_INCLUDED_ITEMS,
                totalItems = 66,
                totalPages = 2,
                hasNextPage = true,
                hasPreviousPage = false,
            ),
        )

        val included = assembler.manyPaged("states", { page }) { it }

        assertEquals(IncludeSpec.MAX_INCLUDED_ITEMS, included?.size)
        assertEquals(listOf("states"), assembler.truncatedIncludes())
    }

    @Test
    fun `a store-capped collection that fits is not reported as cut`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("states", allowed))
        val page = Page(
            items = listOf("s0", "s1"),
            metadata = PageMetadata(
                page = 0,
                size = IncludeSpec.MAX_INCLUDED_ITEMS,
                totalItems = 2,
                totalPages = 1,
                hasNextPage = false,
                hasPreviousPage = false,
            ),
        )

        assertEquals(listOf("s0", "s1"), assembler.manyPaged("states", { page }) { it })
        assertNull(assembler.truncatedIncludes())
    }

    @Test
    fun `each cut collection is reported once`() {
        val assembler = IncludeAssembler(IncludeSpec.parse("currencies,states", allowed))
        val tooMany = List(IncludeSpec.MAX_INCLUDED_ITEMS + 1) { "x$it" }

        assembler.many("currencies", { tooMany }) { it }
        assembler.many("states", { tooMany }) { it }

        assertEquals(listOf("currencies", "states"), assembler.truncatedIncludes())
    }
}
