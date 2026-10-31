package dev.minorcell.slate.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.SharedPreferencesStore
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.data.system.TvSources
import dev.minorcell.slate.domain.model.Builtin
import dev.minorcell.slate.ui.screen.FilesScreen
import dev.minorcell.slate.ui.screen.GalleryScreen
import dev.minorcell.slate.ui.screen.HomeScreen
import dev.minorcell.slate.ui.screen.PlayerScreen
import dev.minorcell.slate.ui.screen.SettingsScreen
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.ScreenMetrics
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 一个 Activity 里叠放桌面和内置功能。返回先关掉最上面的一层，不退出桌面。
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stack = ArrayDeque<AppScreen>()

    private lateinit var stage: FrameLayout
    private lateinit var home: HomeScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        applyHeaderSizes()
        registerBack()

        val repository = AppRepository(this)
        val prefs = SharedPreferencesStore(this)
        stage = findViewById(R.id.stage)
        home =
            HomeScreen(
                activity = this,
                repository = repository,
                prefs = prefs,
                sources = TvSources(this),
                tiles = TileFactory(this),
                scope = scope,
                launch = ::launch,
                openBuiltin = ::openBuiltin,
            )
        scope.launch {
            repository.packageChanges().collect { home.reload() }
        }
        home.reload()
    }

    /** 顶栏字号按窗口高度取比例，换面板或密度时不会忽大忽小。 */
    private fun applyHeaderSizes() {
        val metrics = ScreenMetrics(this)
        val sizes =
            mapOf(
                R.id.eyebrow to EYEBROW_RATIO,
                R.id.title to TITLE_RATIO,
                R.id.clock to CLOCK_RATIO,
                R.id.date to DATE_RATIO,
            )
        sizes.forEach { (id, ratio) ->
            findViewById<TextView>(id).setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.h(ratio).toFloat())
        }
        val header = findViewById<ViewGroup>(R.id.header)
        header.setPadding(
            header.paddingLeft,
            metrics.h(HEADER_TOP_RATIO),
            header.paddingRight,
            metrics.h(HEADER_BOTTOM_RATIO),
        )
    }

    override fun onResume() {
        super.onResume()
        (stack.lastOrNull() as? SettingsScreen)?.reload()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && stack.isEmpty()) home.requestInitialFocus()
    }

    override fun onPause() {
        home.onHostPause()
        stack.lastOrNull()?.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        stack.forEach { it.destroy() }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        while (stack.isNotEmpty()) pop()
        home.onBack()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        goBack()
    }

    @Deprecated("Use the platform callback until the activity moves off the framework result API.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != StorageAccess.REQUEST_CODE) return
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        stack.lastOrNull()?.onPermissionResult(granted)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val top = stack.lastOrNull()
        if ((top ?: home).onDirection(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun openBuiltin(builtin: Builtin) {
        val screen =
            when (builtin) {
                Builtin.Settings -> SettingsScreen(this, ::launch)
                Builtin.Gallery -> GalleryScreen(this, LocalMedia(this), scope)
                Builtin.Player -> PlayerScreen(this, LocalMedia(this), TileFactory(this), scope)
                Builtin.Files -> FilesScreen(this, LocalMedia(this), scope, ::launch)
            }
        push(screen)
    }

    private fun push(screen: AppScreen) {
        homeView().descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        stage.addView(
            screen.view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        stack.addLast(screen)
        screen.onShow()
    }

    private fun pop() {
        val screen = stack.removeLastOrNull() ?: return
        screen.destroy()
        stage.removeView(screen.view)
        if (stack.isEmpty()) {
            homeView().descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            home.onShow()
        } else {
            stack.last().onShow()
        }
    }

    private fun goBack() {
        val top = stack.lastOrNull()
        if (top != null) {
            if (!top.onBack()) pop()
            return
        }
        home.onBack()
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "cannot launch", e)
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun homeView(): ViewGroup = findViewById(R.id.home)

    private fun registerBack() {
        if (Build.VERSION.SDK_INT < 33) return
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        ) { goBack() }
    }

    private companion object {
        const val TAG = "HomeActivity"
        const val EYEBROW_RATIO = 0.017f
        const val TITLE_RATIO = 0.042f
        const val CLOCK_RATIO = 0.058f
        const val DATE_RATIO = 0.019f
        const val HEADER_TOP_RATIO = 0.022f
        const val HEADER_BOTTOM_RATIO = 0.014f
    }
}
