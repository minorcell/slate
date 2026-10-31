package dev.minorcell.slate.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.domain.model.AppEntry

/**
 * 把一个 [AppEntry] 渲染成桌面卡片。
 *
 * 只负责视觉：banner 优先，缺失时退回图标 + 名称。
 * 交互（点击、长按、焦点）由 Activity 绑定。
 */
class TileFactory(private val activity: Activity) {
    fun create(app: AppEntry): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.tile, null, false)
        view.tag = app
        view.clipToOutline = true
        view.findViewById<TextView>(R.id.label).text = app.label

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

    private data class Visuals(val banner: Drawable?, val icon: Drawable?)

    private companion object {
        const val TAG = "TileFactory"
    }
}
