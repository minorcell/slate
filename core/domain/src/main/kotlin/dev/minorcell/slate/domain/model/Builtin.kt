package dev.minorcell.slate.domain.model

enum class Builtin(val id: String) {
    Settings("builtin.settings"),
    Gallery("builtin.gallery"),
    Player("builtin.player"),
    Store("builtin.store"),
    ;

    companion object {
        fun fromId(id: String): Builtin? = entries.find { it.id == id }
    }
}
