package dev.minorcell.slate.data

import android.content.Context
import dev.minorcell.slate.domain.home.PinnedApps
import dev.minorcell.slate.domain.model.Builtin

/**
 * 桌面需要持久化的用户状态。
 *
 * 接口化是为了让上层不依赖 SharedPreferences 的具体实现，测试时可替换为内存实现。
 */
interface Preferences {
    var order: List<String>
    var pinned: List<String>
    var lastFocused: String?
}

class SharedPreferencesStore(context: Context) : Preferences {
    private val store = context.getSharedPreferences("slate", Context.MODE_PRIVATE)

    override var order: List<String>
        get() = ids(KEY_ORDER)
        set(value) = store.edit().putString(KEY_ORDER, value.joinToString(SEPARATOR)).apply()

    override var pinned: List<String>
        get() = if (!store.contains(KEY_PINNED)) PinnedApps.defaultIds else ids(KEY_PINNED)
        set(value) = store.edit().putString(KEY_PINNED, value.joinToString(SEPARATOR)).apply()

    override var lastFocused: String?
        get() = store.getString(KEY_LAST_FOCUSED, null)?.let(Builtin::normalizeId)
        set(value) = store.edit().putString(KEY_LAST_FOCUSED, value).apply()

    private fun ids(key: String): List<String> =
        store.getString(key, "").orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }.map(Builtin::normalizeId)

    private companion object {
        const val KEY_ORDER = "order"
        const val KEY_PINNED = "pinned"
        const val KEY_LAST_FOCUSED = "last_focused"
        const val SEPARATOR = "\u0000"
    }
}
