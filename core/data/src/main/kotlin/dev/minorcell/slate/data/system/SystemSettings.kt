package dev.minorcell.slate.data.system

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 桌面不自带设置页。右上角的设置和应用列表里的「系统设置」打开同一页：电视自己的设置。
 *
 * 这台海信上，`android.settings.mtk.SETTINGS` 和 `com.android.settings.Settings`
 * 会进安卓原生设置。电视设置是 `com.android.tv.settings` 的启动页
 * （`com.hisense.systemsettings.MainActivity`），和应用图标是同一个入口。
 */
object SystemSettings {
    fun intent(context: Context): Intent? {
        val chosen = television(context) ?: Intent(Settings.ACTION_SETTINGS)
        return chosen
            .takeIf { it.resolveActivity(context.packageManager) != null }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun television(context: Context): Intent? {
        val query =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(TV_SETTINGS)
        val activity =
            context.packageManager
                .queryIntentActivities(query, 0)
                .firstOrNull()
                ?.activityInfo
                ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(activity.packageName, activity.name)
    }

    private const val TV_SETTINGS = "com.android.tv.settings"
}
