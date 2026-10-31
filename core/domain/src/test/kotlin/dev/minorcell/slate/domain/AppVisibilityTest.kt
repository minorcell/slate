package dev.minorcell.slate.domain

import dev.minorcell.slate.domain.model.AppEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class AppVisibilityTest {
    private fun app(pkg: String) = AppEntry(pkg, "$pkg.Main", pkg)

    @Test
    fun `hidden apps are filtered out`() {
        val apps = listOf(app("a"), app("b"), app("c"))
        val visible = AppVisibility.visible(apps, setOf("b"))
        assertEquals(listOf("a", "c"), visible.map { it.packageName })
    }

    @Test
    fun `hide and unhide are inverse`() {
        val hidden = AppVisibility.hide(emptySet(), "a")
        assertEquals(setOf("a"), hidden)
        assertEquals(emptySet(), AppVisibility.unhide(hidden, "a"))
    }

    @Test
    fun `hiding twice keeps a single entry`() {
        val hidden = AppVisibility.hide(AppVisibility.hide(emptySet(), "a"), "a")
        assertEquals(setOf("a"), hidden)
    }
}
