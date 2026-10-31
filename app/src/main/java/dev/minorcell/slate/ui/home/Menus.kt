package dev.minorcell.slate.ui.home

import android.app.Activity
import android.app.AlertDialog
import dev.minorcell.slate.R

/**
 * 桌面上用到的对话框。与 Activity 解耦，便于复用和后续替换实现。
 */
object Menus {
    enum class Action { MOVE, INFO, UNINSTALL }

    fun showActions(
        activity: Activity,
        title: String,
        actions: List<Action>,
        onAction: (Action) -> Unit,
    ): AlertDialog {
        val labels = actions.map { activity.getString(it.label) }.toTypedArray()
        return AlertDialog.Builder(activity, DIALOG_THEME)
            .setTitle(title)
            .setItems(labels) { _, which -> onAction(actions[which]) }
            .show()
    }

    private val Action.label: Int
        get() =
            when (this) {
                Action.MOVE -> R.string.move
                Action.INFO -> R.string.app_info
                Action.UNINSTALL -> R.string.uninstall
            }

    private const val DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert
}
