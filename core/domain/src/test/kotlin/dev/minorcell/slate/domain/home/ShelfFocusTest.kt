package dev.minorcell.slate.domain.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        val ids = listOf("sources", "pinned", "apps")
        assertEquals("pinned", ShelfFocus.adjacent(ids, "sources", 1))
        assertEquals("apps", ShelfFocus.adjacent(ids, "apps", 1))
    }

    @Test
    fun `nearest picks the closest center`() {
        val ids = listOf("a", "b", "c")
        assertEquals("b", ShelfFocus.nearest(ids, listOf(0, 100, 200), 120))
    }
}

class PinnedAppsTest {
    @Test
    fun `add stops at the limit`() {
        val full = (1..PinnedApps.LIMIT).map { "p$it" }
        assertNull(PinnedApps.add(full, "extra"))
        assertEquals(listOf("a", "b"), PinnedApps.add(listOf("a"), "b"))
    }

    @Test
    fun `remove drops one id`() {
        assertEquals(listOf("a"), PinnedApps.remove(listOf("a", "b"), "b"))
    }
}
