package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import dev.minorcell.slate.R
import dev.minorcell.slate.ui.settings.SettingsPanel
import dev.minorcell.slate.ui.shell.AppScreen

class SettingsScreen(
    activity: Activity,
    launch: (Intent) -> Unit,
) : AppScreen {
    private val panelView = LayoutInflater.from(activity).inflate(R.layout.settings_panel, null, false)
    private val panel = SettingsPanel(panelView, launch)

    override val view: View = panelView

    init {
        panelView.visibility = View.VISIBLE
        panelView.isClickable = true
    }

    override fun onShow() {
        panelView.post { panel.show() }
    }

    fun reload() {
        panel.reload()
    }
}
