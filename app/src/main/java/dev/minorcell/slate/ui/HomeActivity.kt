package dev.minorcell.slate.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.SharedPreferencesStore
import dev.minorcell.slate.data.system.SystemSettings
import dev.minorcell.slate.ui.home.FrostPlate
import dev.minorcell.slate.ui.home.HomeScreen
import dev.minorcell.slate.ui.home.TileFactory
import dev.minorcell.slate.ui.shell.ScreenMetrics
import dev.minorcell.slate.ui.theme.SlateType
import dev.minorcell.slate.wallpaper.Backdrop
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 桌面本身。二级页面不留在这里：设置、应用信息和卸载都交给系统。
 */
class HomeActivity : Activity() {
    /**
     * 界面只有中文。固定到简体中文，顶栏的星期和系统弹出的授权页才会一致；
     * 不提供语言切换。
     */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.SIMPLIFIED_CHINESE
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val io =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "slate-io").apply { isDaemon = true }
        }
    private var stopWatchingPackages: (() -> Unit)? = null

    private lateinit var home: HomeScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        applyChrome()
        mountBackdrop()
        registerBack()

        val repository = AppRepository(this)
        val prefs = SharedPreferencesStore(this)
        home =
            HomeScreen(
                activity = this,
                repository = repository,
                prefs = prefs,
                tiles = TileFactory(this),
                io = io,
                launch = { launch(it) },
            )
        findViewById<View>(R.id.settings).setOnClickListener { openSettings() }
        stopWatchingPackages = repository.watchPackages { home.reload() }
        home.reload()
    }

    /** 字号和顶栏留白按窗口高度取比例，换面板或密度时不会忽大忽小。 */
    private fun applyChrome() {
        val metrics = ScreenMetrics(this)
        findViewById<TextView>(R.id.kicker).apply {
            typeface = SlateType.hint()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(KICKER_RATIO).toFloat())
        }
        findViewById<TextView>(R.id.title).apply {
            typeface = SlateType.title()
            letterSpacing = SlateType.TITLE_TRACKING
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(TITLE_RATIO).toFloat())
        }
        findViewById<TextView>(R.id.clock).apply {
            typeface = SlateType.clock()
            letterSpacing = SlateType.CLOCK_TRACKING
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(CLOCK_RATIO).toFloat())
        }
        findViewById<TextView>(R.id.date).apply {
            typeface = SlateType.date()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(DATE_RATIO).toFloat())
        }
        val button = metrics.h(SETTINGS_RATIO)
        findViewById<ImageButton>(R.id.settings).apply {
            layoutParams =
                layoutParams.apply {
                    width = button
                    height = button
                }
        }
        val edge = metrics.w(EDGE_RATIO)
        val top = metrics.h(TOPBAR_TOP_RATIO)
        findViewById<View>(R.id.topbar).setPadding(edge, top, edge, 0)
        val clockBlock = findViewById<View>(R.id.clock_block)
        val params = clockBlock.layoutParams as FrameLayout.LayoutParams
        params.marginEnd = metrics.w(CLOCK_END_RATIO)
        clockBlock.layoutParams = params
        val pane = findViewById<View>(R.id.shelf_pane)
        val paneParams = pane.layoutParams as LinearLayout.LayoutParams
        val bottom = metrics.h(SHELF_BOTTOM_RATIO)
        paneParams.setMargins(edge, 0, edge, bottom)
        pane.layoutParams = paneParams
    }

    /** 壁纸按窗口像素画。磨砂底板等这张图好了再铺，避免用一块纯色挡住背景。 */
    private fun mountBackdrop() {
        val stage = findViewById<View>(R.id.stage)
        val backdrop = findViewById<ImageView>(R.id.backdrop)
        val plate = findViewById<View>(R.id.shelf_pane)
        stage.post {
            if (stage.width == 0 || stage.height == 0) return@post
            io.execute {
                val picture = Backdrop.render(stage.width, stage.height)
                stage.post {
                    if (isDestroyed) return@post
                    backdrop.setImageBitmap(picture.sharp)
                    FrostPlate.track(plate, backdrop, picture.frost, resources.getDimension(R.dimen.glass_radius))
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) home.requestInitialFocus()
    }

    override fun onPause() {
        home.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopWatchingPackages?.invoke()
        io.shutdown()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        home.onBack()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        home.onBack()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (home.onDirection(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun openSettings() {
        val intent = SystemSettings.intent(this)
        if (intent == null) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
            return
        }
        launch(intent)
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "cannot launch", e)
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerBack() {
        if (Build.VERSION.SDK_INT < BACK_CALLBACK_SDK) return
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        ) { home.onBack() }
    }

    private companion object {
        const val TAG = "HomeActivity"
        const val BACK_CALLBACK_SDK = 33
        const val KICKER_RATIO = 0.022f
        const val TITLE_RATIO = 0.04f
        const val CLOCK_RATIO = 0.11f
        const val DATE_RATIO = 0.024f
        const val SETTINGS_RATIO = 0.056f
        const val EDGE_RATIO = 0.044f
        const val TOPBAR_TOP_RATIO = 0.07f
        const val CLOCK_END_RATIO = 0.1f
        const val SHELF_BOTTOM_RATIO = 0.04f
    }
}
