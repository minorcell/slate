package dev.minorcell.slate.domain

enum class Direction { LEFT, RIGHT, UP, DOWN }

/**
 * 网格焦点移动的纯计算。返回新的下标；无法移动时返回原下标。
 */
object FocusNavigator {
    fun next(current: Int, direction: Direction, columns: Int, count: Int): Int {
        if (count <= 0 || current !in 0 until count || columns <= 0) return current
        val row = current / columns
        val column = current % columns
        val target =
            when (direction) {
                Direction.LEFT -> if (column > 0) current - 1 else current
                Direction.RIGHT ->
                    if (column < columns - 1 && current + 1 < count) current + 1 else current
                Direction.UP -> if (row > 0) current - columns else current
                Direction.DOWN -> if (current + columns < count) current + columns else current
            }
        return target.coerceIn(0, count - 1)
    }

    /**
     * 焦点需要回到首行时，找到同一列最靠上的元素。
     */
    fun firstRowIndex(column: Int, columns: Int, count: Int): Int {
        if (count <= 0 || columns <= 0) return 0
        return column.coerceIn(0, columns - 1).coerceAtMost(count - 1)
    }
}
