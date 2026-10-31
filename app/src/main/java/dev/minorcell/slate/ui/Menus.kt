package dev.minorcell.slate.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import dev.minorcell.slate.R
import dev.minorcell.slate.domain.model.AppEntry

/**
 * 桌面上用到的对话框。与 Activity 解耦，便于复用和后续替换实现。
 */
object Menus {
    enum class Action { MOVE, HIDE, INFO, UNINSTALL }

    fun showActions(
        activity: Activity,
        app: AppEntry,
        onAction: (Action) -> Unit,
    ) {
        val labels =
            arrayOf(
                activity.getString(R.string.move),
                activity.getString(R.string.hide),
                activity.getString(R.string.app_info),
                activity.getString(R.string.uninstall),
            )
        AlertDialog.Builder(activity, DIALOG_THEME)
            .setTitle(app.label)
            .setItems(labels) { _, which -> onAction(Action.values()[which]) }
            .show()
    }

    fun showHidden(
        activity: Activity,
        hidden: List<AppEntry>,
        onRestore: (AppEntry) -> Unit,
    ) {
        if (hidden.isEmpty()) {
            Toast.makeText(activity, R.string.no_hidden, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity, DIALOG_THEME)
            .setTitle(R.string.unhide)
            .setItems(hidden.map { it.label }.toTypedArray()) { _, which -> onRestore(hidden[which]) }
            .show()
    }

    private const val DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert
}
