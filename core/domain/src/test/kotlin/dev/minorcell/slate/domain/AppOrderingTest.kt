package dev.minorcell.slate.domain

import dev.minorcell.slate.domain.model.AppEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class AppOrderingTest {
    private fun app(pkg: String, label: String = pkg) = AppEntry(pkg, "$pkg.Main", label)

    @Test
    fun `saved order wins over label`() {
        val apps = listOf(app("c"), app("a"), app("b"))
        val sorted = AppOrdering.sort(apps, listOf("c", "b", "a"))
        assertEquals(listOf("c", "b", "a"), sorted.map { it.packageName })
    }

    @Test
    fun `new apps fall back to label order`() {
        val apps = listOf(app("z", "Zebra"), app("a", "Apple"), app("m", "Mango"))
        val sorted = AppOrdering.sort(apps, listOf("m"))
        assertEquals(listOf("m", "a", "z"), sorted.map { it.packageName })
    }

    @Test
    fun `unknown saved packages are ignored`() {
        val apps = listOf(app("a", "A"))
        val sorted = AppOrdering.sort(apps, listOf("gone", "a"))
        assertEquals(listOf("a"), sorted.map { it.packageName })
    }

    @Test
    fun `move reorders items`() {
        assertEquals(listOf("b", "a", "c"), AppOrdering.move(listOf("a", "b", "c"), 0, 1))
    }

    @Test
    fun `move ignores out of range`() {
        val items = listOf("a", "b")
        assertEquals(items, AppOrdering.move(items, 0, 5))
        assertEquals(items, AppOrdering.move(items, -1, 0))
    }
}
