package dev.minorcell.slate.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.Preferences
import dev.minorcell.slate.domain.AppOrdering
import dev.minorcell.slate.domain.home.ShelfFocus
import dev.minorcell.slate.domain.model.AppEntry
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.ScreenMetrics
import dev.minorcell.slate.ui.theme.SlateType
import java.util.concurrent.Executor

/**
 * 一行应用。左右在行内移动，不循环。
 * 焦点上的应用把名称写在时钟下面。设置按钮打开系统设置，不进入本应用的页面。
 * 方法集中在这一处：再拆类会多占安装包。
 */
@Suppress("TooManyFunctions")
class HomeScreen(
    private val activity: Activity,
    private val repository: AppRepository,
    private val prefs: Preferences,
    private val tiles: TileFactory,
    private val io: Executor,
    private val launch: (Intent) -> Unit,
) : AppScreen {
    private val metrics = ScreenMetrics(activity)
    private val settingsButton: View? = activity.findViewById(R.id.settings)
    private val shelves = activity.findViewById<LinearLayout>(R.id.shelves)
    private val scroller = activity.findViewById<ScrollView>(R.id.scroll)
    private val title = activity.findViewById<TextView>(R.id.title)
    private val kicker = activity.findViewById<TextView>(R.id.kicker)
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
        io.execute {
            val installed = repository.load()
            view.post {
                if (ticket != loadTicket || activity.isDestroyed) return@post
                render(installed)
            }
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
        val shelf = shelf(shelfKey) ?: firstShelf() ?: return
        val id = focusedId ?: if (shelf.ids.isEmpty()) null else shelf.ids[0]
        if (id != null) focus(shelf, id)
    }

    override fun onHostPause() {
        val focused = focusedId
        if (focused != null) prefs.lastFocused = focused
    }

    override fun onDirection(event: KeyEvent): Boolean =
        when {
            dialog?.isShowing == true -> false
            movingId != null -> moveKey(event)
            event.action != KeyEvent.ACTION_DOWN -> false
            settingsButton?.hasFocus() == true -> settingsKey(event)
            else -> directionDown(event)
        }

    private fun directionDown(event: KeyEvent): Boolean =
        when (event.keyCode) {
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

    private fun settingsKey(event: KeyEvent): Boolean {
        val directional = lateral(event.keyCode) || vertical(event.keyCode)
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            val shelf = shelf(shelfKey) ?: firstShelf()
            val id = focusedOrFirst(shelf)
            if (shelf != null && id != null) focus(shelf, id)
        }
        return directional
    }

    override fun onBack(): Boolean {
        if (movingId != null) endMove() else focusFirst()
        return true
    }

    private fun focusFirst() {
        val target = firstShelf() ?: return
        if (target.ids.isEmpty()) return
        val first = target.ids[0]
        if (shelfKey != target.key || focusedId != first) focus(target, first)
    }

    private fun render(installed: List<AppEntry>) {
        val ordered = AppOrdering.sort(installed, prefs.order)
        apps.clear()
        var index = 0
        while (index < ordered.size) {
            val app = ordered[index]
            apps[app.packageName] = app
            index++
        }
        rows.clear()
        shelves.removeAllViews()
        addApps(APPS, R.string.shelf_apps, ordered)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        if (rows.isEmpty()) clearHero()
        restore()
    }

    private fun addApps(
        key: String,
        title: Int,
        entries: List<AppEntry>,
    ) {
        if (entries.isEmpty()) return
        val shelf = inflateShelf(key, title)
        var index = 0
        while (index < entries.size) {
            val app = entries[index]
            shelf.ids.add(app.packageName)
            shelf.row.addView(appTile(app))
            index++
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
            typeface = SlateType.shelf()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(SHELF_TITLE_RATIO).toFloat())
        }
        block.setPadding(
            metrics.w(SHELF_INSET_RATIO),
            metrics.h(SHELF_TOP_RATIO),
            metrics.w(SHELF_INSET_RATIO),
            metrics.h(SHELF_BOTTOM_RATIO),
        )
        val scroll = block.findViewById<HorizontalScrollView>(R.id.shelf_scroll)
        (scroll.layoutParams as ViewGroup.MarginLayoutParams).topMargin = metrics.h(SHELF_GAP_RATIO)
        val row = block.findViewById<LinearLayout>(R.id.shelf_row)
        // 焦点会放大卡片，行和父容器都不能裁掉超出边界的描边。
        row.clipChildren = false
        row.clipToPadding = false
        // 两端留白：首尾卡片放大后不会顶出屏幕，最后一项也能完整滚进来。
        row.setPadding(0, metrics.h(ROW_VERTICAL_RATIO), metrics.w(ROW_END_RATIO), metrics.h(ROW_VERTICAL_RATIO))
        return Shelf(key, block, scroll, row, mutableListOf())
    }

    private fun appTile(app: AppEntry): View = tiles.create(app).apply { bind(this, app.packageName, app) }

    private fun bind(
        view: View,
        id: String,
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
            present(id)
        }
        view.setOnClickListener {
            if (movingId != null) endMove() else activate(id)
        }
        view.setOnLongClickListener {
            if (movingId == null && app != null) showMenu(shelfOf(view)?.key ?: APPS, app)
            true
        }
    }

    private fun present(id: String) {
        val app = apps[id] ?: return
        val moving = movingId == id
        kicker.visibility = if (moving) View.VISIBLE else View.GONE
        if (moving) kicker.setText(R.string.move_hint)
        title.text = app.label
        var index = 0
        while (index < rows.size) {
            val row = rows[index]
            row.block.alpha = if (row.key == shelfKey) 1f else INACTIVE_SHELF
            index++
        }
    }

    private fun clearHero() {
        kicker.visibility = View.GONE
        title.text = ""
    }

    private fun activate(id: String) {
        val app = apps[id] ?: return
        launch(leanback(app))
    }

    private fun showMenu(
        shelfKey: String,
        app: AppEntry,
    ) {
        val actions = ArrayList<Menus.Action>()
        actions.add(Menus.Action.MOVE)
        actions.add(Menus.Action.INFO)
        actions.add(Menus.Action.UNINSTALL)
        dialog =
            Menus.showActions(activity, app.label, actions) { action ->
                when (action) {
                    Menus.Action.MOVE -> startMove(shelfKey, app.packageName)
                    Menus.Action.INFO -> info(app)
                    Menus.Action.UNINSTALL -> uninstall(app)
                }
            }
    }

    private fun info(app: AppEntry) {
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
        present(id)
    }

    private fun endMove() {
        val key = movingShelf ?: return
        val id = movingId ?: return
        movingShelf = null
        movingId = null
        child(shelf(key), id)?.isSelected = false
        val shelf = shelf(key)
        if (shelf != null) prefs.order = ArrayList(shelf.ids)
        present(id)
    }

    private fun moveKey(event: KeyEvent): Boolean {
        val key = event.keyCode
        val handled = lateral(key) || vertical(key) || okKey(key)
        if (!handled) return false
        if (event.action == KeyEvent.ACTION_DOWN && lateral(key)) {
            moveWithin(if (key == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && okKey(key)) endMove()
        return true
    }

    private fun moveWithin(delta: Int) {
        val shelf = movingShelf?.let { shelf(it) } ?: return
        val from = shelf.ids.indexOf(movingId)
        val to = from + delta
        if (from < 0 || to < 0 || to >= shelf.ids.size) return
        val next = AppOrdering.move(ArrayList(shelf.ids), from, to)
        shelf.ids.clear()
        shelf.ids.addAll(next)
        val child = shelf.row.getChildAt(from)
        shelf.row.removeViewAt(from)
        shelf.row.addView(child, to)
        child.requestFocus()
    }

    private fun focusHorizontal(delta: Int) {
        val shelf = shelf(shelfKey) ?: firstShelf() ?: return
        val current = focusedId ?: if (shelf.ids.isEmpty()) return else shelf.ids[0]
        focus(shelf, ShelfFocus.horizontal(shelf.ids, current, delta))
    }

    private fun focusVertical(delta: Int) {
        if (rows.isEmpty()) return
        val keys = ArrayList<String>(rows.size)
        var index = 0
        while (index < rows.size) {
            keys.add(rows[index].key)
            index++
        }
        val nextKey = ShelfFocus.adjacent(keys, shelfKey, delta)
        if (nextKey == shelfKey) {
            if (delta < 0) settingsButton?.requestFocus()
        } else {
            val next = shelf(nextKey)
            if (next != null) focusNearest(next)
        }
    }

    private fun focusNearest(next: Shelf) {
        val current = currentView()
        val target = if (current == null) 0 else centerX(current)
        val centers = ArrayList<Int>(next.ids.size)
        var tileIndex = 0
        while (tileIndex < next.ids.size) {
            val tile = child(next, next.ids[tileIndex])
            centers.add(if (tile == null) 0 else centerX(tile))
            tileIndex++
        }
        val id = ShelfFocus.nearest(next.ids, centers, target)
        if (id.isNotEmpty()) focus(next, id)
    }

    private fun restore() {
        val remembered = focusedId ?: prefs.lastFocused
        var match: Shelf? = null
        var index = 0
        while (index < rows.size) {
            val row = rows[index]
            if (remembered != null && remembered in row.ids) {
                match = row
                break
            }
            index++
        }
        val shelf = match ?: firstShelf() ?: return
        val id = if (match != null && remembered != null) remembered else shelf.ids[0]
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
                bottom > y + height -> {
                    val next = bottom - height
                    scroller.smoothScrollTo(0, if (next < 0) 0 else next)
                }
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
            shelf.scroll.smoothScrollTo(if (target < 0) 0 else target, 0)
        }
    }

    private fun currentView(): View? = child(shelf(shelfKey), focusedId)

    private fun child(
        shelf: Shelf?,
        id: String?,
    ): View? {
        val row = shelf ?: return null
        val index = row.ids.indexOf(id)
        return if (index < 0) null else row.row.getChildAt(index)
    }

    private fun shelf(key: String?): Shelf? {
        var index = 0
        while (key != null && index < rows.size) {
            if (rows[index].key == key) return rows[index]
            index++
        }
        return null
    }

    private fun shelfOf(view: View): Shelf? {
        var index = 0
        while (index < rows.size) {
            if (rows[index].row.indexOfChild(view) >= 0) return rows[index]
            index++
        }
        return null
    }

    private fun firstShelf(): Shelf? = if (rows.isEmpty()) null else rows[0]

    private fun focusedOrFirst(shelf: Shelf?): String? {
        val focused = focusedId
        return when {
            shelf == null || shelf.ids.isEmpty() -> null
            focused != null && focused in shelf.ids -> focused
            else -> shelf.ids[0]
        }
    }

    private fun lateral(key: Int): Boolean = key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT

    private fun vertical(key: Int): Boolean = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN

    private fun okKey(key: Int): Boolean = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER

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
        const val APPS = "apps"
        const val FOCUS_SCALE = 1.07f
        const val ANIMATION_MS = 220L
        const val SHELF_TITLE_RATIO = 0.028f
        const val SHELF_GAP_RATIO = 0.008f
        const val SHELF_TOP_RATIO = 0.02f
        const val SHELF_BOTTOM_RATIO = 0.016f
        const val SHELF_INSET_RATIO = 0.02f
        const val ROW_END_RATIO = 0.06f
        const val ROW_VERTICAL_RATIO = 0.016f
        const val TILE_EDGE_RATIO = 0.02f
        const val INACTIVE_SHELF = 0.84f
    }
}
