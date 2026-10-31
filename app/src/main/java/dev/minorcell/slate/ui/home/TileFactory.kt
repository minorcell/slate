package dev.minorcell.slate.ui.home

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import dev.minorcell.slate.R
import dev.minorcell.slate.domain.model.AppEntry
import dev.minorcell.slate.ui.shell.ScreenMetrics

private const val WIDTH_RATIO = 0.17f
private const val GAP_RATIO = 0.016f
private const val ICON_RATIO = 0.5f

/**
 * 一张应用卡。居中放启动图标，名称不写在卡片上。
 *
 * 不用 android:banner 铺满。云视听小电视这类 banner 就是图标加一行字，
 * 铺进 16:9 之后看起来像把图标放大了好几倍。没有 banner 的系统应用本来就是居中图标，以那个为准。
 */
class TileFactory(private val activity: Activity) {
    private val metrics = ScreenMetrics(activity)

    fun create(app: AppEntry): View {
        val view = inflate()
        view.contentDescription = app.label
        view.findViewById<ImageView>(R.id.icon).setImageDrawable(icon(app.packageName))
        return view
    }

    private fun icon(packageName: String): Drawable? =
        try {
            val info = activity.packageManager.getApplicationInfo(packageName, 0)
            info.loadIcon(activity.packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "package not found: $packageName", e)
            null
        }

    private fun inflate(): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.tile, null, false)
        val width = metrics.w(WIDTH_RATIO)
        val height = width * POSTER_HEIGHT / POSTER_WIDTH
        val gap = metrics.h(GAP_RATIO)
        val inset = metrics.h(RING_INSET_RATIO).coerceAtLeast(1)
        view.setPadding(inset, inset, inset, inset)
        val radius = activity.resources.getDimension(R.dimen.tile_radius)
        view.findViewById<View>(R.id.surface).apply {
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        outlineView: View,
                        outline: Outline,
                    ) {
                        outline.setRoundRect(0, 0, outlineView.width, outlineView.height, radius)
                    }
                }
            clipToOutline = true
        }
        view.layoutParams =
            ViewGroup.MarginLayoutParams(width, height).apply {
                setMargins(gap, gap, gap, gap)
            }
        val iconSize = (height * ICON_RATIO).toInt()
        view.findViewById<ImageView>(R.id.icon).layoutParams =
            FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        return view
    }

    private companion object {
        const val TAG = "TileFactory"
        const val RING_INSET_RATIO = 0.007f
        const val POSTER_WIDTH = 16
        const val POSTER_HEIGHT = 9
    }
}
