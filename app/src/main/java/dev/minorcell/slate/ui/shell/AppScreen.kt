package dev.minorcell.slate.ui.shell

import android.view.KeyEvent
import android.view.View

interface AppScreen {
    val view: View

    fun onShow() {}

    fun onDirection(event: KeyEvent): Boolean = false

    fun onBack(): Boolean = false

    fun onHostPause() {}


    fun destroy() {}
}
