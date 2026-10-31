package dev.minorcell.slate.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.Preferences
import dev.minorcell.slate.data.SharedPreferencesStore
import dev.minorcell.slate.domain.AppOrdering
import dev.minorcell.slate.domain.AppVisibility
import dev.minorcell.slate.domain.Direction
import dev.minorcell.slate.domain.FocusNavigator
import dev.minorcell.slate.domain.model.AppEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 电视桌面主界面：加载应用、渲染网格、处理焦点与长按菜单。
 *
 * 业务规则（排序、隐藏、焦点计算）都在 core:domain，
 * 卡片渲染在 [TileFactory]，对话框在 [Menus]。
 */
class HomeActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: AppRepository
    private lateinit var prefs: Preferences
    private lateinit var tileFactory: TileFactory

    private lateinit var grid: GridLayout
    private lateinit var title: TextView
    private lateinit var empty: TextView

    private var apps: List<AppEntry> = emptyList()
    private var visible: List<AppEntry> = emptyList()
    private var moving: AppEntry? = null
    private var focusedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        repository = AppRepository(this)
        prefs = SharedPreferencesStore(this)
        tileFactory = TileFactory(this)

        grid = findViewById(R.id.grid)
        title = findViewById(R.id.title)
        empty = findViewById(R.id.empty)

        findViewById<View>(R.id.settings).setOnClickListener {
            launch(Intent(Settings.ACTION_SETTINGS))
        }
        findViewById<View>(R.id.hidden).setOnClickListener { showHidden() }
        listOf(R.id.settings, R.id.hidden).forEach { id ->
            findViewById<View>(id).setOnFocusChangeListener { view, focused ->
                if (focused) title.text = view.contentDescription
            }
        }

        scope.launch {
            repository.packageChanges().collect { reload() }
        }
        reload()
    }

    override fun onPause() {
        super.onPause()
        focusedPackage?.let { prefs.lastFocused = it }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (moving != null) endMove()
        grid.getChildAt(0)?.requestFocus()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (moving != null) endMove()
    }

    private fun reload() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.load() }
            apps = AppOrdering.sort(loaded, prefs.order)
            render()
        }
    }

    private fun render() {
        visible = AppVisibility.visible(apps, prefs.hidden)
        grid.removeAllViews()
        visible.forEach { grid.addView(tile(it)) }
        empty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        val remembered = prefs.lastFocused
        val index =
            visible.indexOfFirst { it.packageName == focusedPackage }
                .takeIf { it >= 0 }
                ?: visible.indexOfFirst { it.packageName == remembered }.coerceAtLeast(0)
        grid.getChildAt(index)?.let { view ->
            view.post { if (!view.hasFocus()) view.requestFocus() }
        }
    }

    private fun tile(app: AppEntry): View =
        tileFactory.create(app).apply {
            setOnFocusChangeListener { focusedView, focused ->
                val scale = if (focused) FOCUS_SCALE else 1f
                focusedView.animate().scaleX(scale).scaleY(scale).setDuration(ANIMATION_MS).start()
                if (focused) {
                    focusedPackage = app.packageName
                    title.text = if (moving === app) getString(R.string.move_hint) else app.label
                }
            }
            setOnClickListener { if (moving != null) endMove() else open(app) }
            setOnLongClickListener {
                if (moving == null) showMenu(app)
                true
            }
        }

    private fun open(app: AppEntry) {
        launch(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "cannot launch intent: $intent", e)
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMenu(app: AppEntry) {
        Menus.showActions(this, app) { action ->
            when (action) {
                Menus.Action.MOVE -> startMove(app)
                Menus.Action.HIDE -> {
                    prefs.hidden = AppVisibility.hide(prefs.hidden, app.packageName)
                    render()
                }
                Menus.Action.INFO ->
                    launch(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", app.packageName, null),
                        ),
                    )
                Menus.Action.UNINSTALL ->
                    launch(
                        Intent(
                            Intent.ACTION_DELETE,
                            Uri.fromParts("package", app.packageName, null),
                        ),
                    )
            }
        }
    }

    private fun showHidden() {
        Menus.showHidden(this, apps.filter { it.packageName in prefs.hidden }) { app ->
            prefs.hidden = AppVisibility.unhide(prefs.hidden, app.packageName)
            render()
        }
    }

    private fun startMove(app: AppEntry) {
        moving = app
        tileOf(app)?.isSelected = true
        title.setText(R.string.move_hint)
    }

    private fun endMove() {
        val app = moving ?: return
        moving = null
        tileOf(app)?.isSelected = false
        title.text = app.label
        prefs.order = visible.map { it.packageName }
    }

    private fun tileOf(app: AppEntry): View? =
        (0 until grid.childCount).map { grid.getChildAt(it) }.find { it.tag === app }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val app = moving
        if (event.action != KeyEvent.ACTION_DOWN || app == null) {
            return super.dispatchKeyEvent(event)
        }
        if (event.keyCode in FINISH_KEYS) {
            endMove()
        } else {
            moveTile(app, event.keyCode.toDirection())
        }
        return true
    }

    private fun moveTile(
        app: AppEntry,
        direction: Direction?,
    ) {
        val from = visible.indexOf(app)
        val to = direction?.let { FocusNavigator.next(from, it, COLUMNS, visible.size) } ?: from
        if (from < 0 || to == from) return
        visible = AppOrdering.move(visible, from, to)
        val view = grid.getChildAt(from)
        grid.removeViewAt(from)
        grid.addView(view, to)
        view.requestFocus()
    }

    private companion object {
        const val TAG = "HomeActivity"
        const val COLUMNS = 4
        const val FOCUS_SCALE = 1.085f
        const val ANIMATION_MS = 120L
        val FINISH_KEYS =
            setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK)
    }
}

private fun Int.toDirection(): Direction? =
    when (this) {
        KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
        else -> null
    }
