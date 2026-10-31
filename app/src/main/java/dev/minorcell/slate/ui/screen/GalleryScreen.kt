package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.data.media.MediaItem
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.FocusScale
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryScreen(
    private val activity: Activity,
    private val media: LocalMedia,
    private val scope: CoroutineScope,
) : AppScreen {
    private val root = LayoutInflater.from(activity).inflate(R.layout.strip_screen, null, false)
    private val strip = root.findViewById<LinearLayout>(R.id.strip)
    private val empty = root.findViewById<TextView>(R.id.empty)
    private val viewer = root.findViewById<ImageView>(R.id.viewer)
    private var generation = 0
    private var asked = false
    private var viewerOpen = false
    private var full: Bitmap? = null

    override val view: View = root

    init {
        root.findViewById<TextView>(R.id.screen_title).setText(R.string.gallery)
        root.findViewById<TextView>(R.id.screen_hint).setText(R.string.gallery_hint)
        empty.setOnClickListener { askOrLoad() }
    }

    override fun onShow() {
        if (StorageAccess.granted(activity, StorageAccess.Kind.Images)) {
            load()
        } else {
            showDenied()
            if (!asked) {
                asked = true
                StorageAccess.request(activity, StorageAccess.Kind.Images)
            }
        }
    }

    override fun onDirection(event: KeyEvent): Boolean {
        if (!viewerOpen) return false
        return event.keyCode in DIRECTION_KEYS
    }

    override fun onBack(): Boolean {
        if (!viewerOpen) return false
        closeViewer()
        return true
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) load() else showDenied()
    }

    override fun destroy() {
        generation += 1
        closeViewer()
        recycleThumbs()
    }

    private fun askOrLoad() {
        if (StorageAccess.granted(activity, StorageAccess.Kind.Images)) {
            load()
        } else {
            StorageAccess.request(activity, StorageAccess.Kind.Images)
        }
    }

    private fun load() {
        val ticket = ++generation
        empty.visibility = View.GONE
        empty.isFocusable = false
        scope.launch {
            val items = withContext(Dispatchers.IO) { media.images() }
            if (ticket != generation) return@launch
            recycleThumbs()
            strip.removeAllViews()
            if (items.isEmpty()) {
                showMessage(R.string.empty_photos)
                return@launch
            }
            items.forEach { item -> strip.addView(tile(item)) }
            strip.getChildAt(0)?.requestFocus()
            items.forEachIndexed { index, item -> decode(ticket, index, item) }
        }
    }

    private suspend fun decode(
        ticket: Int,
        index: Int,
        item: MediaItem,
    ) {
        val bitmap = withContext(Dispatchers.IO) { media.thumb(item.uri, THUMB_WIDTH) }
        if (ticket != generation || bitmap == null) {
            bitmap?.recycle()
            return
        }
        val tile = strip.getChildAt(index) ?: return
        tile.findViewById<ImageView>(R.id.banner).apply {
            setImageBitmap(bitmap)
            visibility = View.VISIBLE
        }
        tile.findViewById<View>(R.id.fallback).visibility = View.GONE
    }

    private fun tile(item: MediaItem): View {
        val tile = LayoutInflater.from(activity).inflate(R.layout.tile, strip, false)
        tile.findViewById<TextView>(R.id.label).text = item.name
        tile.contentDescription = item.name
        tile.setBackgroundColor(COLOR)
        FocusScale.bind(tile)
        tile.setOnClickListener { open(item) }
        return tile
    }

    private fun open(item: MediaItem) {
        val ticket = generation
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { media.thumb(item.uri, VIEW_WIDTH) } ?: return@launch
            if (ticket != generation) {
                bitmap.recycle()
                return@launch
            }
            viewer.setImageDrawable(null)
            full?.recycle()
            full = bitmap
            viewer.setImageBitmap(bitmap)
            viewer.visibility = View.VISIBLE
            viewerOpen = true
        }
    }

    private fun closeViewer() {
        viewerOpen = false
        viewer.setImageDrawable(null)
        viewer.visibility = View.GONE
        full?.recycle()
        full = null
    }

    private fun recycleThumbs() {
        for (index in 0 until strip.childCount) {
            val image = strip.getChildAt(index).findViewById<ImageView>(R.id.banner)
            val bitmap = image.drawable
            image.setImageDrawable(null)
            if (bitmap is BitmapDrawable) bitmap.bitmap.recycle()
        }
    }

    private fun showDenied() {
        showMessage(R.string.allow_files)
    }

    private fun showMessage(text: Int) {
        strip.removeAllViews()
        empty.setText(text)
        empty.visibility = View.VISIBLE
        empty.isFocusable = true
        empty.isClickable = true
        empty.requestFocus()
    }

    private companion object {
        const val THUMB_WIDTH = 360
        const val VIEW_WIDTH = 1280
        val COLOR = 0xFF241C16.toInt()
        val DIRECTION_KEYS =
            setOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
            )
    }
}
