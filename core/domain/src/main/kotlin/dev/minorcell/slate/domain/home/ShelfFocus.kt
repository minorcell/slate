package dev.minorcell.slate.domain.home

object ShelfFocus {
    fun horizontal(
        ids: List<String>,
        current: String,
        delta: Int,
    ): String {
        if (ids.isEmpty()) return current
        val index = ids.indexOf(current).takeIf { it >= 0 } ?: return ids.first()
        val next = index + delta
        if (next !in ids.indices) return current
        return ids[next]
    }

    fun adjacent(
        ids: List<String>,
        current: String,
        delta: Int,
    ): String {
        if (ids.isEmpty()) return current
        val index = ids.indexOf(current).coerceAtLeast(0)
        return ids[(index + delta).coerceIn(0, ids.lastIndex)]
    }

    fun nearest(
        ids: List<String>,
        centers: List<Int>,
        target: Int,
    ): String {
        if (ids.isEmpty()) return ""
        var best = 0
        var bestDistance = Int.MAX_VALUE
        centers.forEachIndexed { index, center ->
            val distance = kotlin.math.abs(center - target)
            if (distance < bestDistance) {
                best = index
                bestDistance = distance
            }
        }
        return ids[best.coerceIn(0, ids.lastIndex)]
    }
}
