package dev.minorcell.slate.domain

import dev.minorcell.slate.domain.model.AppEntry

/**
 * 桌面顺序规则：用户保存过的顺序优先，其余按名称排列。
 */
object AppOrdering {
    fun sort(apps: List<AppEntry>, savedOrder: List<String>): List<AppEntry> {
        val rank = savedOrder.withIndex().associate { (index, pkg) -> pkg to index }
        return apps.sortedWith(
            compareBy<AppEntry> { rank[it.packageName] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() },
        )
    }

    /**
     * 把 [from] 位置的应用移到 [to]，越界时原样返回。
     */
    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        val result = items.toMutableList()
        result.add(to, result.removeAt(from))
        return result
    }
}
