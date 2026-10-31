package dev.minorcell.slate.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FocusNavigatorTest {
    // 3 列，5 个元素：
    // 0 1 2
    // 3 4
    private val columns = 3
    private val count = 5

    private fun move(
        current: Int,
        direction: Direction,
    ) = FocusNavigator.next(current, direction, columns, count)

    @Test
    fun `left moves within a row`() {
        assertEquals(1, move(2, Direction.LEFT))
    }

    @Test
    fun `left at row start stays`() {
        assertEquals(3, move(3, Direction.LEFT))
    }

    @Test
    fun `right moves within a row`() {
        assertEquals(2, move(1, Direction.RIGHT))
    }

    @Test
    fun `right at row end stays`() {
        assertEquals(2, move(2, Direction.RIGHT))
    }

    @Test
    fun `right does not wrap into an empty cell`() {
        assertEquals(4, move(4, Direction.RIGHT))
    }

    @Test
    fun `up moves by one row`() {
        assertEquals(1, move(4, Direction.UP))
    }

    @Test
    fun `up on first row stays`() {
        assertEquals(0, move(0, Direction.UP))
    }

    @Test
    fun `down moves by one row`() {
        assertEquals(4, move(1, Direction.DOWN))
    }

    @Test
    fun `down on last row stays`() {
        assertEquals(3, move(3, Direction.DOWN))
    }

    @Test
    fun `empty grid keeps zero`() {
        assertEquals(0, FocusNavigator.next(0, Direction.RIGHT, columns, 0))
    }
}
