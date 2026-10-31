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
import dev.minorcell.slate.domain.model.Builtin
import dev.minorcell.slate.ui.shell.ScreenMetrics

/**
 * 一行里卡片的尺寸，全部用屏幕比例表示。
 *
 * 信号行最小，常用行最大，应用行居中；比例来自原型在 1920×1080 上的取值。
 */
enum class TileSpec(
    val widthRatio: Float,
    val heightRatio: Float,
) {
    Source(0.170f, 0.115f),
    Pinned(0.220f, 0.195f),
    App(0.168f, 0.150f),
}

private const val GAP_RATIO = 0.012f
private const val ICON_RATIO = 0.30f
private const val ICON_LIFT_RATIO = 0.07f
private const val LABEL_RATIO = 0.026f

/**
 * 把一个 [AppEntry] 渲染成桌面卡片。
 *
 * 只负责视觉：banner 优先，缺失时退回图标 + 名称。
 * 内置应用与信号源没有 banner，统一使用 `tile_bg` 的暗色表面加线稿图标。
 * 交互（点击、长按、焦点）由 [dev.minorcell.slate.ui.screen.HomeScreen] 绑定。
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
        val builtin = app.builtin
        if (builtin != null) {
            icon(view).setImageResource(iconOf(builtin))
            return view
        }

        val visuals = visualsFor(app.packageName)
        val bannerView = view.findViewById<ImageView>(R.id.banner)
        if (visuals.banner != null) {
            bannerView.setImageDrawable(visuals.banner)
            bannerView.visibility = View.VISIBLE
            view.findViewById<View>(R.id.fallback).visibility = View.GONE
        } else {
            icon(view).setImageDrawable(visuals.icon)
        }
        return view
    }

    /** 没有对应 AppEntry 的卡片，例如电视输入源。 */
    fun plain(
        label: String,
        iconRes: Int,
        spec: TileSpec = TileSpec.App,
    ): View {
        val view = inflate(spec)
        view.findViewById<TextView>(R.id.label).text = label
        view.contentDescription = label
        icon(view).setImageResource(iconRes)
        return view
    }

    private fun icon(view: View): ImageView = view.findViewById(R.id.icon)

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

    private fun iconOf(builtin: Builtin): Int =
        when (builtin) {
            Builtin.Settings -> R.drawable.ic_builtin_settings
            Builtin.Gallery -> R.drawable.ic_builtin_gallery
            Builtin.Player -> R.drawable.ic_builtin_player
            Builtin.Files -> R.drawable.ic_builtin_files
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
        icon(view).apply {
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
