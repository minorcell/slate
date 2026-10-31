package dev.minorcell.slate.domain.home

import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfFocusTest {
    @Test
    fun `horizontal stops at the ends`() {
        val ids = listOf("a", "b", "c")
        assertEquals("b", ShelfFocus.horizontal(ids, "a", 1))
        assertEquals("a", ShelfFocus.horizontal(ids, "a", -1))
        assertEquals("c", ShelfFocus.horizontal(ids, "c", 1))
    }

    @Test
    fun `adjacent shelf does not wrap`() {
        val ids = listOf("a", "b", "c")
        assertEquals("b", ShelfFocus.adjacent(ids, "a", 1))
        assertEquals("c", ShelfFocus.adjacent(ids, "c", 1))
    }

    @Test
    fun `nearest picks the closest center`() {
        val ids = listOf("a", "b", "c")
        assertEquals("b", ShelfFocus.nearest(ids, listOf(0, 100, 200), 120))
    }
}
