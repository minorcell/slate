package dev.minorcell.slate.domain.home

object PinnedApps {
    const val LIMIT = 7

    /** 桌面不自带应用，常用行默认是空的，等用户自己放。 */
    val defaultIds: List<String> = emptyList()

    fun add(
        pinned: List<String>,
        id: String,
    ): List<String>? {
        if (id in pinned) return pinned
        if (pinned.size >= LIMIT) return null
        return pinned + id
    }

    fun remove(
        pinned: List<String>,
        id: String,
    ): List<String> = pinned.filterNot { it == id }
}
