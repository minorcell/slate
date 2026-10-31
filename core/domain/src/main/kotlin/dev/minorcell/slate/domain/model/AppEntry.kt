package dev.minorcell.slate.domain.model

/**
 * 一个可在电视桌面启动的应用。
 *
 * 只包含纯数据，不持有 Drawable、Intent 等 Android 类型，
 * 这样整个 domain 模块都能在 JVM 上直接单测。
 */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val builtin: Builtin? = null,
)
