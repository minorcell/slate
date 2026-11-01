package dev.minorcell.slate.data

import android.content.Context

/**
 * 桌面需要持久化的用户状态。
 *
 * 接口化是为了让上层不依赖 SharedPreferences 的具体实现，测试时可替换为内存实现。
 */
interface Preferences {
    var order: List<String>
    var lastFocused: String?
}

class SharedPreferencesStore(context: Context) : Preferences {
    private val store = context.getSharedPreferences("slate", Context.MODE_PRIVATE)

    override var order: List<String>
        get() = ids(KEY_ORDER)
        set(value) {
            store.edit().putString(KEY_ORDER, join(value)).apply()
        }

    override var lastFocused: String?
        get() = store.getString(KEY_LAST_FOCUSED, null)
        set(value) {
            store.edit().putString(KEY_LAST_FOCUSED, value).apply()
        }

    private fun join(values: List<String>): String {
        val out = StringBuilder()
        var index = 0
        while (index < values.size) {
            if (index > 0) out.append(SEPARATOR)
            out.append(values[index])
            index++
        }
        return out.toString()
    }

    private fun ids(key: String): List<String> {
        val raw = store.getString(key, "") ?: ""
        val out = ArrayList<String>()
        var start = 0
        while (start <= raw.length) {
            val at = raw.indexOf(SEPARATOR, start)
            val end = if (at < 0) raw.length else at
            if (end > start) out.add(raw.substring(start, end))
            if (at < 0) break
            start = at + 1
        }
        return out
    }

    private companion object {
        const val KEY_ORDER = "order"
        const val KEY_LAST_FOCUSED = "last_focused"
        const val SEPARATOR = '\u0000'
    }
}
