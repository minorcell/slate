package dev.minorcell.slate.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import dev.minorcell.slate.domain.model.AppEntry

/**
 * 从系统读取可在电视桌面启动的应用。
 *
 * 只返回纯数据 [AppEntry]；图标与 banner 由 UI 层按包名自行加载，
 * 避免把 Drawable 带进领域层。
 */
class AppRepository(private val context: Context) {
    private val packageManager: PackageManager get() = context.packageManager

    /**
     * 同时收 LEANBACK 与普通 LAUNCHER 两类入口。
     *
     * 电视应用一般只声明 leanback，侧载的手机应用往往只有普通 launcher；
     * 只查 leanback 会让刚装好的侧载应用在桌面上消失。同一个包优先用 leanback 入口。
     */
    fun load(): List<AppEntry> {
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>()
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER, seen, result)
        collect(Intent.CATEGORY_LAUNCHER, seen, result)
        return result
    }

    private fun collect(
        category: String,
        seen: MutableSet<String>,
        result: MutableList<AppEntry>,
    ) {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        val resolved = packageManager.queryIntentActivities(query, 0)
        var index = 0
        while (index < resolved.size) {
            val resolve = resolved[index]
            val activity = resolve.activityInfo
            if (activity.packageName != context.packageName && seen.add(activity.packageName)) {
                result.add(
                    AppEntry(
                        packageName = activity.packageName,
                        activityName = activity.name,
                        label = resolve.loadLabel(packageManager).toString(),
                    ),
                )
            }
            index++
        }
    }

    /**
     * 应用安装、卸载、更新时回调。返回值用来解除注册。
     */
    fun watchPackages(onChange: () -> Unit): () -> Unit {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    onChange()
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
        context.registerReceiver(receiver, filter)
        return { context.unregisterReceiver(receiver) }
    }
}
