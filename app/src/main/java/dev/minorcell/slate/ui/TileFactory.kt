package dev.minorcell.slate.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.domain.model.AppEntry
import dev.minorcell.slate.ui.shell.ScreenMetrics

/**
 * 一行里卡片的尺寸，全部用屏幕比例表示。常用行略大，应用行略小。
 */
enum class TileSpec(
    val widthRatio: Float,
    val heightRatio: Float,
) {
    Pinned(0.220f, 0.195f),
    App(0.168f, 0.150f),
}

private const val GAP_RATIO = 0.012f
private const val ICON_RATIO = 0.30f
private const val ICON_LIFT_RATIO = 0.07f
private const val LABEL_RATIO = 0.026f

/**
 * 把一个 [AppEntry] 渲染成桌面卡片：有 banner 用 banner，否则图标加名称。
 */
class TileFactory(private val activity: Activity) {
    private val metrics = ScreenMetrics(activity)

    fun create(
        app: AppEntry,
        spec: TileSpec = TileSpec.App,
    ): View {
        val view = inflate(spec)
        view.tag = app
        view.findViewById<TextView>(R.id.label).text = app.label
        view.contentDescription = app.label
        val visuals = visualsFor(app.packageName)
        val bannerView = view.findViewById<ImageView>(R.id.banner)
        if (visuals.banner != null) {
            bannerView.setImageDrawable(visuals.banner)
            bannerView.visibility = View.VISIBLE
            view.findViewById<View>(R.id.fallback).visibility = View.GONE
        } else {
            view.findViewById<ImageView>(R.id.icon).setImageDrawable(visuals.icon)
        }
        return view
    }

    private fun visualsFor(packageName: String): Visuals =
        try {
            val info = activity.packageManager.getApplicationInfo(packageName, 0)
            Visuals(
                info.loadBanner(activity.packageManager),
                info.loadIcon(activity.packageManager),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "package not found: $packageName", e)
            Visuals(null, null)
        }

    private fun inflate(spec: TileSpec): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.tile, null, false)
        view.clipToOutline = true
        val width = metrics.w(spec.widthRatio)
        val height = metrics.h(spec.heightRatio)
        val gap = metrics.h(GAP_RATIO)
        view.layoutParams =
            ViewGroup.MarginLayoutParams(width, height).apply {
                setMargins(gap, gap, gap, gap)
            }
        val iconSize = (height * ICON_RATIO).toInt()
        view.findViewById<ImageView>(R.id.icon).apply {
            layoutParams =
                FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
            translationY = -(height * ICON_LIFT_RATIO)
        }
        view.findViewById<TextView>(R.id.label)
            .setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(LABEL_RATIO).toFloat())
        return view
    }

    private data class Visuals(val banner: Drawable?, val icon: Drawable?)

    private companion object {
        const val TAG = "TileFactory"
    }
}
