package dev.minorcell.slate.ui

import android.app.Activity
import android.app.AlertDialog
import dev.minorcell.slate.R
import dev.minorcell.slate.domain.model.AppEntry

/**
 * 桌面上用到的对话框。与 Activity 解耦，便于复用和后续替换实现。
 */
object Menus {
    enum class Action { MOVE, INFO, UNINSTALL }

    fun showActions(
        activity: Activity,
        app: AppEntry,
        onAction: (Action) -> Unit,
    ) {
        val labels =
            arrayOf(
                activity.getString(R.string.move),
                activity.getString(R.string.app_info),
                activity.getString(R.string.uninstall),
            )
        AlertDialog.Builder(activity, DIALOG_THEME)
            .setTitle(app.label)
            .setItems(labels) { _, which -> onAction(Action.values()[which]) }
            .show()
    }

    private const val DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert
}
