package dev.minorcell.slate.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingRangesTest {
    @Test
    fun `step moves by stride and stops at the ends`() {
        assertEquals(15, SettingRanges.step(10, 1, 0, 100, 5))
        assertEquals(0, SettingRanges.step(3, -1, 0, 100, 5))
        assertEquals(100, SettingRanges.step(96, 1, 0, 100, 5))
    }

    @Test
    fun `catalog starts with the source hint and ends with developer options`() {
        val rows = SettingCatalog.rows()
        assertEquals(SettingCatalog.SOURCE, rows.first().id)
        assertEquals(HandoffTarget.Picture, (rows[1] as SettingRow.Handoff).target)
        assertEquals(HandoffTarget.DeveloperOptions, (rows.last() as SettingRow.Handoff).target)
        assertEquals(6, rows.size)
    }
}
