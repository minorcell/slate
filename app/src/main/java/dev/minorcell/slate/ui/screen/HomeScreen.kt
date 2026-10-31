package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.util.TypedValue
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.Preferences
import dev.minorcell.slate.data.system.SourceKind
import dev.minorcell.slate.data.system.TvSource
import dev.minorcell.slate.data.system.TvSources
import dev.minorcell.slate.domain.AppOrdering
import dev.minorcell.slate.domain.home.PinnedApps
import dev.minorcell.slate.domain.home.ShelfFocus
import dev.minorcell.slate.domain.model.AppEntry
import dev.minorcell.slate.domain.model.Builtin
import dev.minorcell.slate.ui.Menus
import dev.minorcell.slate.ui.TileFactory
import dev.minorcell.slate.ui.TileSpec
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.ScreenMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 三行桌面：信号、常用、应用。上下换行，左右在行内移动，都不循环。
 * 信号行只展示系统报上来的输入源，不在这里切换。
 */
class HomeScreen(
    private val activity: Activity,
    private val repository: AppRepository,
    private val prefs: Preferences,
    private val sources: TvSources,
    private val tiles: TileFactory,
    private val scope: CoroutineScope,
    private val launch: (Intent) -> Unit,
    private val openBuiltin: (Builtin) -> Unit,
) : AppScreen {
    private val metrics = ScreenMetrics(activity)
    private val shelves = activity.findViewById<LinearLayout>(R.id.shelves)
    private val scroller = activity.findViewById<ScrollView>(R.id.scroll)
    private val title = activity.findViewById<TextView>(R.id.title)
    private val empty = activity.findViewById<TextView>(R.id.empty)
    private val rows = mutableListOf<Shelf>()
    private val apps = mutableMapOf<String, AppEntry>()
    private var shelfKey = ""
    private var focusedId: String? = null
    private var movingShelf: String? = null
    private var movingId: String? = null
    private var dialog: AlertDialog? = null
    private var loadTicket = 0

    override val view: View = activity.findViewById(R.id.home)

    fun reload() {
        if (movingId != null) endMove()
        val ticket = ++loadTicket
        scope.launch {
            val installed = withContext(Dispatchers.IO) { repository.load() }
            val inputs = withContext(Dispatchers.IO) { sources.load() }
            if (ticket != loadTicket) return@launch
            render(installed, inputs)
        }
    }

    override fun onShow() {
        if (rows.isNotEmpty()) restore()
    }

    /**
     * 窗口拿到焦点后再请求一次。数据是异步加载的，首次渲染时桌面还没有焦点，
     * 不补这一下，第一次按键会被系统的默认焦点搜索吃掉。
     */
    fun requestInitialFocus() {
        if (rows.isEmpty()) return
        val shelf = shelf(shelfKey) ?: rows.first()
        val id = focusedId ?: shelf.ids.firstOrNull() ?: return
        focus(shelf, id)
    }

    override fun onHostPause() {
        focusedId?.let { prefs.lastFocused = it }
    }

    override fun onDirection(event: KeyEvent): Boolean {
        if (dialog?.isShowing == true) return false
        val moving = movingId
        if (moving != null) return moveKey(event)
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                focusHorizontal(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                focusHorizontal(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                focusVertical(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                focusVertical(1)
                true
            }
            else -> false
        }
    }

    override fun onBack(): Boolean {
        if (movingId != null) {
            endMove()
            return true
        }
        val target = shelf(PINNED) ?: rows.firstOrNull() ?: return true
        val first = target.ids.firstOrNull() ?: return true
        if (shelfKey != target.key || focusedId != first) focus(target, first)
        return true
    }

    private fun render(
        installed: List<AppEntry>,
        inputs: List<TvSource>,
    ) {
        val ordered = AppOrdering.sort(builtins() + installed, prefs.order)
        apps.clear()
        ordered.forEach { apps[it.packageName] = it }
        rows.clear()
        shelves.removeAllViews()
        addSources(inputs)
        addApps(PINNED, R.string.shelf_pinned, pinnedEntries())
        addApps(APPS, R.string.shelf_apps, ordered)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        restore()
    }

    /**
     * 信号行只用系统给的数据。
     *
     * 电视框架没有公开的切换输入接口，TvInputInfo 只提供 createSetupIntent。
     * TIF 里有输入就逐项列出；没有（VIDAA 就是这种）就放一个「信号源」入口，
     * 点了提示按遥控器上的信号源键，不伪造 HDMI 磁贴。
     */
    private fun addSources(inputs: List<TvSource>) {
        val shelf = inflateShelf(SOURCES, R.string.shelf_sources)
        if (inputs.isEmpty()) {
            val id = SOURCE_PREFIX + PANEL_ID
            shelf.ids.add(id)
            shelf.row.addView(signalTile(id))
            shelves.addView(shelf.block)
            rows.add(shelf)
            return
        }
        inputs.forEach { source ->
            val id = SOURCE_PREFIX + source.id
            shelf.ids.add(id)
            shelf.row.addView(sourceTile(id, source))
        }
        shelves.addView(shelf.block)
        rows.add(shelf)
    }

    private fun addApps(
        key: String,
        title: Int,
        entries: List<AppEntry>,
    ) {
        if (entries.isEmpty()) return
        val shelf = inflateShelf(key, title)
        val spec = if (key == PINNED) TileSpec.Pinned else TileSpec.App
        entries.forEach { app ->
            shelf.ids.add(app.packageName)
            shelf.row.addView(appTile(app, spec))
        }
        shelves.addView(shelf.block)
        rows.add(shelf)
    }

    private fun inflateShelf(
        key: String,
        title: Int,
    ): Shelf {
        val block = LayoutInflater.from(activity).inflate(R.layout.shelf, shelves, false)
        block.findViewById<TextView>(R.id.shelf_title).apply {
            setText(title)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(SHELF_TITLE_RATIO).toFloat())
        }
        block.setPadding(0, 0, 0, metrics.h(SHELF_BOTTOM_RATIO))
        val scroll = block.findViewById<HorizontalScrollView>(R.id.shelf_scroll)
        (scroll.layoutParams as ViewGroup.MarginLayoutParams).topMargin = metrics.h(SHELF_GAP_RATIO)
        val row = block.findViewById<LinearLayout>(R.id.shelf_row)
        // 焦点会放大卡片，行和父容器都不能裁掉超出边界的描边。
        row.clipChildren = false
        row.clipToPadding = false
        // 两端留白：首尾卡片放大后不会顶出屏幕，最后一项也能完整滚进来。
        row.setPadding(metrics.w(ROW_START_RATIO), 0, metrics.w(ROW_END_RATIO), 0)
        return Shelf(key, block, scroll, row, mutableListOf())
    }

    private fun pinnedEntries(): List<AppEntry> = prefs.pinned.mapNotNull { apps[it] }

    private fun builtins(): List<AppEntry> =
        listOf(
            entry(Builtin.Settings, R.string.settings),
            entry(Builtin.Gallery, R.string.gallery),
            entry(Builtin.Player, R.string.player),
            entry(Builtin.Files, R.string.files),
        )

    private fun entry(
        builtin: Builtin,
        label: Int,
    ): AppEntry = AppEntry(builtin.id, "", activity.getString(label), builtin)

    private fun appTile(
        app: AppEntry,
        spec: TileSpec,
    ): View = tiles.create(app, spec).apply { bind(this, app.packageName, app.label, app) }

    private fun signalTile(id: String): View {
        val label = activity.getString(R.string.source_signal)
        return tiles.plain(label, R.drawable.ic_source_tv, TileSpec.Source).apply {
            bind(this, id, label, null)
        }
    }

    private fun sourceTile(
        id: String,
        source: TvSource,
    ): View =
        tiles.plain(source.label, sourceIcon(source.kind), TileSpec.Source).apply {
            bind(this, id, source.label, null)
        }

    private fun sourceIcon(kind: SourceKind): Int =
        when (kind) {
            SourceKind.HDMI -> R.drawable.ic_source_hdmi
            SourceKind.AV -> R.drawable.ic_source_av
            SourceKind.TUNER, SourceKind.OTHER -> R.drawable.ic_source_tv
        }

    private fun bind(
        view: View,
        id: String,
        label: String,
        app: AppEntry?,
    ) {
        view.tag = id
        view.setOnFocusChangeListener { focusedView, focused ->
            val scale = if (focused) FOCUS_SCALE else 1f
            focusedView.animate().scaleX(scale).scaleY(scale).setDuration(ANIMATION_MS).start()
            if (!focused) return@setOnFocusChangeListener
            shelfKey = shelfOf(focusedView)?.key ?: shelfKey
            focusedId = id
            prefs.lastFocused = id
            title.text = if (movingId == id) activity.getString(R.string.move_hint) else label
        }
        view.setOnClickListener {
            if (movingId != null) endMove() else activate(id)
        }
        view.setOnLongClickListener {
            if (movingId == null && app != null) showMenu(shelfOf(view)?.key ?: APPS, app)
            true
        }
    }

    private fun activate(id: String) {
        if (id.startsWith(SOURCE_PREFIX)) {
            val inputId = id.removePrefix(SOURCE_PREFIX)
            if (inputId == PANEL_ID) {
                Toast.makeText(activity, R.string.source_hint, Toast.LENGTH_LONG).show()
            } else {
                sources.setupIntent(inputId)?.let(launch)
            }
            return
        }
        val app = apps[id] ?: return
        val builtin = app.builtin
        if (builtin != null) {
            openBuiltin(builtin)
        } else {
            launch(leanback(app))
        }
    }

    private fun showMenu(
        shelfKey: String,
        app: AppEntry,
    ) {
        val actions = actionsFor(shelfKey, app)
        dialog =
            Menus.showActions(activity, app.label, actions) { action ->
                when (action) {
                    Menus.Action.PIN -> pin(app.packageName)
                    Menus.Action.UNPIN -> unpin(app.packageName)
                    Menus.Action.MOVE -> startMove(shelfKey, app.packageName)
                    Menus.Action.INFO -> info(app)
                    Menus.Action.UNINSTALL -> uninstall(app)
                }
            }
    }

    private fun actionsFor(
        shelfKey: String,
        app: AppEntry,
    ): List<Menus.Action> {
        val actions = mutableListOf<Menus.Action>()
        val pinned = app.packageName in prefs.pinned
        actions += if (shelfKey == PINNED || pinned) Menus.Action.UNPIN else Menus.Action.PIN
        actions += Menus.Action.MOVE
        actions += Menus.Action.INFO
        if (app.builtin == null) actions += Menus.Action.UNINSTALL
        return actions
    }

    private fun pin(id: String) {
        val next = PinnedApps.add(prefs.pinned, id)
        if (next == null) {
            Toast.makeText(activity, R.string.pin_limit, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.pinned = next
        reload()
    }

    private fun unpin(id: String) {
        prefs.pinned = PinnedApps.remove(prefs.pinned, id)
        reload()
    }

    private fun info(app: AppEntry) {
        if (app.builtin != null) {
            Toast.makeText(activity, R.string.builtin_info, Toast.LENGTH_SHORT).show()
            return
        }
        launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null),
            ),
        )
    }

    private fun uninstall(app: AppEntry) {
        launch(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
    }

    private fun leanback(app: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setClassName(app.packageName, app.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun startMove(
        key: String,
        id: String,
    ) {
        movingShelf = key
        movingId = id
        child(shelf(key), id)?.isSelected = true
        title.setText(R.string.move_hint)
    }

    private fun endMove() {
        val key = movingShelf ?: return
        val id = movingId ?: return
        movingShelf = null
        movingId = null
        child(shelf(key), id)?.isSelected = false
        val shelf = shelf(key)
        if (shelf != null && key == PINNED) prefs.pinned = shelf.ids.toList()
        if (shelf != null && key == APPS) prefs.order = shelf.ids.toList()
        title.text = apps[id]?.label ?: title.text
    }

    private fun moveKey(event: KeyEvent): Boolean {
        val key = event.keyCode
        val handled = key in LATERAL || key in VERTICAL || key in OK_KEYS
        if (!handled) return false
        if (event.action == KeyEvent.ACTION_DOWN && key in LATERAL) {
            moveWithin(if (key == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && key in OK_KEYS) endMove()
        return true
    }

    private fun moveWithin(delta: Int) {
        val shelf = shelf(movingShelf ?: return) ?: return
        val from = shelf.ids.indexOf(movingId)
        val to = from + delta
        if (from < 0 || to !in shelf.ids.indices) return
        val next = AppOrdering.move(shelf.ids.toList(), from, to)
        shelf.ids.clear()
        shelf.ids.addAll(next)
        val child = shelf.row.getChildAt(from)
        shelf.row.removeViewAt(from)
        shelf.row.addView(child, to)
        child.requestFocus()
    }

    private fun focusHorizontal(delta: Int) {
        val shelf = shelf(shelfKey) ?: rows.firstOrNull() ?: return
        val current = focusedId ?: shelf.ids.firstOrNull() ?: return
        focus(shelf, ShelfFocus.horizontal(shelf.ids, current, delta))
    }

    private fun focusVertical(delta: Int) {
        if (rows.isEmpty()) return
        val nextKey = ShelfFocus.adjacent(rows.map { it.key }, shelfKey, delta)
        if (nextKey == shelfKey) return
        val next = shelf(nextKey) ?: return
        val target = currentView()?.let { centerX(it) } ?: 0
        val centers = next.ids.map { id -> child(next, id)?.let { centerX(it) } ?: 0 }
        val id = ShelfFocus.nearest(next.ids, centers, target)
        if (id.isEmpty()) return
        focus(next, id)
    }

    private fun restore() {
        val remembered = focusedId ?: prefs.lastFocused
        val match = rows.firstOrNull { remembered != null && remembered in it.ids }
        val shelf = match ?: rows.firstOrNull() ?: return
        val id = if (match != null && remembered != null) remembered else shelf.ids.first()
        focus(shelf, id)
    }

    private fun focus(
        shelf: Shelf,
        id: String,
    ) {
        val target = child(shelf, id) ?: return
        shelfKey = shelf.key
        focusedId = id
        target.post {
            target.requestFocus()
            revealTile(shelf, target)
            reveal(shelf.block)
        }
    }

    private fun reveal(block: View) {
        scroller.post {
            val top = block.top
            val bottom = block.bottom
            val y = scroller.scrollY
            val height = scroller.height
            when {
                top < y -> scroller.smoothScrollTo(0, top)
                bottom > y + height -> scroller.smoothScrollTo(0, (bottom - height).coerceAtLeast(0))
            }
        }
    }

    private fun revealTile(
        shelf: Shelf,
        view: View,
    ) {
        shelf.scroll.post {
            // 可视区要去掉 ScrollView 自己的左右 padding，
            // 否则会少滚一个 padding 的距离，最后一张卡被顶到屏幕外。
            val viewport = shelf.scroll.width - shelf.scroll.paddingLeft - shelf.scroll.paddingRight
            val left = view.left
            val right = view.right
            val x = shelf.scroll.scrollX
            val margin = metrics.w(TILE_EDGE_RATIO)
            val target =
                when {
                    left - margin < x -> left - margin
                    right + margin > x + viewport -> right + margin - viewport
                    else -> return@post
                }
            shelf.scroll.smoothScrollTo(target.coerceAtLeast(0), 0)
        }
    }

    private fun currentView(): View? = child(shelf(shelfKey), focusedId)

    private fun child(
        shelf: Shelf?,
        id: String?,
    ): View? {
        if (shelf == null || id == null) return null
        val index = shelf.ids.indexOf(id)
        if (index < 0) return null
        return shelf.row.getChildAt(index)
    }

    private fun shelf(key: String?): Shelf? = rows.firstOrNull { it.key == key }

    private fun shelfOf(view: View): Shelf? = rows.firstOrNull { it.row.indexOfChild(view) >= 0 }

    private fun centerX(view: View): Int {
        val xy = IntArray(2)
        view.getLocationOnScreen(xy)
        return xy[0] + view.width / 2
    }

    private class Shelf(
        val key: String,
        val block: View,
        val scroll: HorizontalScrollView,
        val row: LinearLayout,
        val ids: MutableList<String>,
    )

    private companion object {
        const val SOURCES = "sources"
        const val PINNED = "pinned"
        const val APPS = "apps"
        const val SOURCE_PREFIX = "tv:"
        const val PANEL_ID = "panel"
        const val FOCUS_SCALE = 1.085f
        const val ANIMATION_MS = 120L
        const val SHELF_TITLE_RATIO = 0.020f
        const val SHELF_GAP_RATIO = 0.006f
        const val SHELF_BOTTOM_RATIO = 0.007f
        const val ROW_END_RATIO = 0.06f
        const val ROW_START_RATIO = 0.012f
        const val TILE_EDGE_RATIO = 0.02f
        val LATERAL = setOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
        val VERTICAL = setOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)
        val OK_KEYS = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)
    }
}
