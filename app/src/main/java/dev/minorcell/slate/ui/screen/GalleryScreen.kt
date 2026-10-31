package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.data.media.MediaItem
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.domain.media.GalleryPaging
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.FocusScale
import dev.minorcell.slate.ui.shell.ScreenMetrics
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 相册：每行 [COLUMNS] 张缩略图，上下滚动翻页；确定看大图，大图里左右换一张。
 */
class GalleryScreen(
    private val activity: Activity,
    private val media: LocalMedia,
    private val scope: CoroutineScope,
) : AppScreen {
    private val root = LayoutInflater.from(activity).inflate(R.layout.grid_screen, null, false)
    private val title = root.findViewById<TextView>(R.id.screen_title)
    private val scroll = root.findViewById<ScrollView>(R.id.grid_scroll)
    private val rows = root.findViewById<LinearLayout>(R.id.grid_rows)
    private val empty = root.findViewById<TextView>(R.id.empty)
    private val viewer = root.findViewById<ImageView>(R.id.viewer)
    private val metrics = ScreenMetrics(activity)

    private var items: List<MediaItem> = emptyList()
    private var viewerIndex = 0
    private var generation = 0
    private var asked = false
    private var viewerOpen = false
    private var full: Bitmap? = null

    override val view: View = root

    init {
        title.setText(R.string.gallery)
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
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> open(GalleryPaging.page(viewerIndex, -1, items.size))
                KeyEvent.KEYCODE_DPAD_RIGHT -> open(GalleryPaging.page(viewerIndex, 1, items.size))
            }
        }
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
        val kind = StorageAccess.Kind.Images
        when {
            StorageAccess.granted(activity, kind) -> load()
            !asked || StorageAccess.canAsk(activity, kind) -> {
                asked = true
                StorageAccess.request(activity, kind)
            }
            else -> StorageAccess.openSettings(activity)
        }
    }

    private fun load() {
        val ticket = ++generation
        empty.visibility = View.GONE
        empty.isFocusable = false
        scope.launch {
            val found = withContext(Dispatchers.IO) { media.images() }
            if (ticket != generation) return@launch
            items = found
            render(ticket)
        }
    }

    private fun render(ticket: Int) {
        recycleThumbs()
        rows.removeAllViews()
        if (items.isEmpty()) {
            showMessage(R.string.empty_photos)
            return
        }
        var index = 0
        items.chunked(COLUMNS).forEach { chunk ->
            rows.addView(row(chunk, index, ticket))
            index += chunk.size
        }
        firstTile()?.requestFocus()
    }

    private fun row(
        chunk: List<MediaItem>,
        startIndex: Int,
        ticket: Int,
    ): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        chunk.forEachIndexed { offset, item ->
            row.addView(tile(item, startIndex + offset, ticket))
        }
        return row
    }

    private fun tile(
        item: MediaItem,
        index: Int,
        ticket: Int,
    ): View {
        val tile = LayoutInflater.from(activity).inflate(R.layout.tile, null, false)
        val gap = metrics.h(GAP_RATIO)
        tile.layoutParams =
            ViewGroup.MarginLayoutParams(cellWidth(), cellHeight()).apply {
                setMargins(gap, gap, gap, gap)
            }
        tile.findViewById<TextView>(R.id.label).text = item.name
        tile.contentDescription = item.name
        FocusScale.bind(tile)
        tile.setOnClickListener { open(index) }
        tile.setOnFocusChangeListener { view, focused ->
            if (focused) scrollIntoView(view)
        }
        decode(ticket, tile, item)
        return tile
    }

    private fun decode(
        ticket: Int,
        tile: View,
        item: MediaItem,
    ) {
        val banner = tile.findViewById<ImageView>(R.id.banner)
        banner.tag = item.uri
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { media.thumb(item.uri, cellWidth()) } ?: return@launch
            if (ticket != generation || banner.tag != item.uri) {
                bitmap.recycle()
                return@launch
            }
            banner.setImageBitmap(bitmap)
            banner.visibility = View.VISIBLE
            tile.findViewById<View>(R.id.fallback).visibility = View.GONE
        }
    }

    private fun open(index: Int) {
        if (items.isEmpty()) return
        val safe = GalleryPaging.page(index, 0, items.size)
        viewerIndex = safe
        val item = items[safe]
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
            title.text = item.name
        }
    }

    private fun closeViewer() {
        viewerOpen = false
        viewer.setImageDrawable(null)
        viewer.visibility = View.GONE
        full?.recycle()
        full = null
        title.setText(R.string.gallery)
    }

    private fun scrollIntoView(view: View) {
        scroll.post {
            val top = view.top
            val bottom = view.bottom
            val y = scroll.scrollY
            val height = scroll.height
            val margin = metrics.h(GAP_RATIO)
            when {
                top - margin < y -> scroll.smoothScrollTo(0, (top - margin).coerceAtLeast(0))
                bottom + margin > y + height -> scroll.smoothScrollTo(0, bottom + margin - height)
            }
        }
    }

    private fun firstTile(): View? {
        val firstRow = rows.getChildAt(0) as? ViewGroup ?: return null
        return firstRow.getChildAt(0)
    }

    private fun recycleThumbs() {
        for (rowIndex in 0 until rows.childCount) {
            val row = rows.getChildAt(rowIndex) as ViewGroup
            for (index in 0 until row.childCount) {
                val image = row.getChildAt(index).findViewById<ImageView>(R.id.banner)
                val bitmap = image.drawable
                image.setImageDrawable(null)
                if (bitmap is BitmapDrawable) bitmap.bitmap.recycle()
            }
        }
    }

    private fun cellWidth(): Int {
        val edge = activity.resources.getDimensionPixelSize(R.dimen.edge)
        val gap = metrics.h(GAP_RATIO)
        val usable = metrics.width - edge * 2 - gap * COLUMNS * 2
        return (usable / COLUMNS).coerceAtLeast(1)
    }

    private fun cellHeight(): Int = (cellWidth() * CELL_RATIO).toInt()

    private fun showDenied() {
        val permanent = !StorageAccess.canAsk(activity, StorageAccess.Kind.Images)
        showMessage(if (permanent) R.string.allow_files_settings else R.string.allow_files)
    }

    private fun showMessage(text: Int) {
        rows.removeAllViews()
        empty.setText(text)
        empty.visibility = View.VISIBLE
        empty.isFocusable = true
        empty.isClickable = true
        empty.requestFocus()
    }

    private companion object {
        const val COLUMNS = 5
        const val GAP_RATIO = 0.012f
        const val CELL_RATIO = 0.62f
        const val VIEW_WIDTH = 1600
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
