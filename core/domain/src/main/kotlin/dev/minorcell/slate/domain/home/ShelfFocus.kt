package dev.minorcell.slate.domain.home

object ShelfFocus {
    fun horizontal(
        ids: List<String>,
        current: String,
        delta: Int,
    ): String {
        if (ids.isEmpty()) return current
        val index = ids.indexOf(current)
        if (index < 0) return ids[0]
        val next = index + delta
        if (next < 0 || next >= ids.size) return current
        return ids[next]
    }

    fun adjacent(
        ids: List<String>,
        current: String,
        delta: Int,
    ): String {
        if (ids.isEmpty()) return current
        var index = ids.indexOf(current)
        if (index < 0) index = 0
        var next = index + delta
        if (next < 0) next = 0
        val last = ids.size - 1
        if (next > last) next = last
        return ids[next]
    }

    fun nearest(
        ids: List<String>,
        centers: List<Int>,
        target: Int,
    ): String {
        if (ids.isEmpty()) return ""
        var best = 0
        var bestDistance = Int.MAX_VALUE
        var index = 0
        while (index < centers.size) {
            val distance = Math.abs(centers[index] - target)
            if (distance < bestDistance) {
                best = index
                bestDistance = distance
            }
            index++
        }
        if (best > ids.size - 1) best = ids.size - 1
        return ids[best]
    }
}
