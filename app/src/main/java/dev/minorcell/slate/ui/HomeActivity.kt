package dev.minorcell.slate.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import dev.minorcell.slate.R
import dev.minorcell.slate.data.AppRepository
import dev.minorcell.slate.data.SharedPreferencesStore
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.data.system.MediaVolume
import dev.minorcell.slate.data.system.ScreenBrightness
import dev.minorcell.slate.data.system.TvSources
import dev.minorcell.slate.domain.model.Builtin
import dev.minorcell.slate.ui.screen.GalleryScreen
import dev.minorcell.slate.ui.screen.HomeScreen
import dev.minorcell.slate.ui.screen.PlayerScreen
import dev.minorcell.slate.ui.screen.SettingsScreen
import dev.minorcell.slate.ui.screen.StoreScreen
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 一个 Activity 里叠放桌面和内置功能。返回先关掉最上面的一层，不退出桌面。
 */
class HomeActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stack = ArrayDeque<AppScreen>()

    private lateinit var stage: FrameLayout
    private lateinit var home: HomeScreen
    private lateinit var volume: MediaVolume
    private lateinit var brightness: ScreenBrightness

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        registerBack()

        val repository = AppRepository(this)
        val prefs = SharedPreferencesStore(this)
        val media = LocalMedia(this)
        volume = MediaVolume(this)
        brightness = ScreenBrightness(this)
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

    override fun onResume() {
        super.onResume()
        (stack.lastOrNull() as? SettingsScreen)?.reload()
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
        val handled = super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP && event.keyCode in VOLUME_KEYS) {
            (stack.lastOrNull() as? SettingsScreen)?.reload()
        }
        return handled
    }

    private fun openBuiltin(builtin: Builtin) {
        val screen =
            when (builtin) {
                Builtin.Settings -> SettingsScreen(this, volume, brightness, ::launch)
                Builtin.Gallery -> GalleryScreen(this, LocalMedia(this), scope)
                Builtin.Player -> PlayerScreen(this, LocalMedia(this), TileFactory(this), scope)
                Builtin.Store -> StoreScreen(this, LocalMedia(this), scope, ::launch)
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
        val VOLUME_KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    }
}
