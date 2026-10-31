package dev.minorcell.slate.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.minorcell.slate.domain.settings.SettingCatalog
import dev.minorcell.slate.domain.settings.SettingRow

/**
 * 安卓的屏幕亮度，[Settings.System.SCREEN_BRIGHTNESS]，范围 0 到 255。
 *
 * 写入需要「修改系统设置」。这和海信图像菜单里的背光不是同一个接口。
 */
class ScreenBrightness(private val context: Context) {
    fun read(): SettingRow.Range {
        val stored =
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                DEFAULT,
            )
        return SettingRow.Range(
            SettingCatalog.BRIGHTNESS,
            stored.coerceIn(0, MAX),
            0,
            MAX,
            STRIDE,
            canWrite(),
        )
    }

    fun write(value: Int): Boolean {
        if (!canWrite()) return false
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value.coerceIn(0, MAX),
            )
        } catch (_: SecurityException) {
            false
        }
    }

    fun grantIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun canWrite(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return Settings.System.canWrite(context)
    }

    private companion object {
        const val MAX = 255
        const val STRIDE = 15
        const val DEFAULT = 102
    }
}
