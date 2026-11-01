package dev.minorcell.slate.domain

import dev.minorcell.slate.domain.model.AppEntry
import java.util.Locale

/**
 * 桌面顺序规则：用户保存过的顺序优先，其余按名称排列。
 */
object AppOrdering {
    fun sort(
        apps: List<AppEntry>,
        savedOrder: List<String>,
    ): List<AppEntry> {
        val rank = HashMap<String, Int>()
        var index = 0
        while (index < savedOrder.size) {
            rank[savedOrder[index]] = index
            index++
        }
        val result = ArrayList(apps)
        result.sortWith { left, right ->
            val byRank = rankOf(rank, left).compareTo(rankOf(rank, right))
            if (byRank != 0) byRank else fold(left.label).compareTo(fold(right.label))
        }
        return result
    }

    /**
     * 把 [from] 位置的应用移到 [to]，越界时原样返回。
     */
    fun <T> move(
        items: List<T>,
        from: Int,
        to: Int,
    ): List<T> =
        when {
            from < 0 || from >= items.size -> items
            to < 0 || to >= items.size -> items
            from == to -> items
            else -> {
                val result = ArrayList(items)
                result.add(to, result.removeAt(from))
                result
            }
        }

    private fun rankOf(
        rank: Map<String, Int>,
        app: AppEntry,
    ): Int = rank[app.packageName] ?: Int.MAX_VALUE

    /** 与 Kotlin `lowercase()` 一样走默认语言，中文名不受影响，英文名大小写仍排在一起。 */
    private fun fold(label: String): String = (label as java.lang.String).toLowerCase(Locale.getDefault())
}
