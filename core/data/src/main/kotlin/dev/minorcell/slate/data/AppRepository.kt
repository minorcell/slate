package dev.minorcell.slate.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import dev.minorcell.slate.domain.model.AppEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 从系统读取可在电视桌面启动的应用。
 *
 * 只返回纯数据 [AppEntry]；图标与 banner 由 UI 层按包名自行加载，
 * 避免把 Drawable 带进领域层。
 */
class AppRepository(private val context: Context) {
    private val packageManager: PackageManager get() = context.packageManager

    fun load(): List<AppEntry> {
        val query =
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return packageManager
            .queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { resolve ->
                val activity = resolve.activityInfo
                AppEntry(
                    packageName = activity.packageName,
                    activityName = activity.name,
                    label = resolve.loadLabel(packageManager).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * 应用安装、卸载、更新时发出事件，用于刷新桌面。
     */
    fun packageChanges(): Flow<Unit> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        trySend(Unit)
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
            awaitClose { context.unregisterReceiver(receiver) }
        }
}
