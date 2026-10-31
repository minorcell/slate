package dev.minorcell.slate.domain.home

import dev.minorcell.slate.domain.model.Builtin

object PinnedApps {
    const val LIMIT = 7

    val defaultIds: List<String> = Builtin.entries.map { it.id }

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
