package dev.minorcell.slate.domain

import dev.minorcell.slate.domain.model.AppEntry

/**
 * 隐藏应用的集合运算。全部为纯函数，方便测试与回放。
 */
object AppVisibility {
    fun visible(apps: List<AppEntry>, hidden: Set<String>): List<AppEntry> =
        apps.filterNot { it.packageName in hidden }

    fun hide(hidden: Set<String>, packageName: String): Set<String> = hidden + packageName

    fun unhide(hidden: Set<String>, packageName: String): Set<String> = hidden - packageName
}
