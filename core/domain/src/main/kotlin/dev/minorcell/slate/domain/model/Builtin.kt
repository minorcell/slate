package dev.minorcell.slate.domain.model

enum class Builtin(val id: String) {
    Settings("builtin.settings"),
    Gallery("builtin.gallery"),
    Player("builtin.player"),
    Files("builtin.files"),
    ;

    companion object {
        /** 旧版本把文件管理器叫“应用商店”，读取已保存的顺序与常用时把旧 id 迁移过来。 */
        fun normalizeId(id: String): String = if (id == LEGACY_STORE_ID) Files.id else id

        fun fromId(id: String): Builtin? = entries.find { it.id == normalizeId(id) }

        private const val LEGACY_STORE_ID = "builtin.store"
    }
}
