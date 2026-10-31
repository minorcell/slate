package dev.minorcell.slate.ui.shell

import android.app.Activity
import android.os.Build

/**
 * 按窗口的真实像素算尺寸。
 *
 * 电视面板和系统密度差异很大：同一台 1080p 电视可能报 1.5 倍密度，
 * 按 dp 写死的尺寸会明显偏小。这里统一用屏幕比例，任何面板换算出来的
 * 相对大小一致。
 */
class ScreenMetrics(activity: Activity) {
    val width: Int
    val height: Int

    init {
        val manager = activity.windowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = manager.defaultDisplay

            @Suppress("DEPRECATION")
            val metrics = android.util.DisplayMetrics().also { display.getRealMetrics(it) }
            width = metrics.widthPixels
            height = metrics.heightPixels
        }
    }

    fun w(ratio: Float): Int = (width * ratio).toInt()

    fun h(ratio: Float): Int = (height * ratio).toInt()
}
