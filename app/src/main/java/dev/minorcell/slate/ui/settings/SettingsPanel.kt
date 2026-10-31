package dev.minorcell.slate.ui.settings

import android.content.Intent
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.minorcell.slate.R
import dev.minorcell.slate.data.system.SettingHandoffs
import dev.minorcell.slate.domain.settings.SettingCatalog
import dev.minorcell.slate.domain.settings.SettingRow

/**
 * 同一窗口里的设置页。
 *
 * 每一项都是电视或系统里已经存在的页面，这里只做入口；
 * 音量交给遥控器音量键，亮度在电视自己的图像设置里。
 */
class SettingsPanel(
    private val panel: View,
    private val launch: (Intent) -> Unit,
) {
    private val rows = panel.findViewById<LinearLayout>(R.id.settings_rows)
    private var openedAt = 0L

    fun show() {
        panel.visibility = View.VISIBLE
        openedAt = SystemClock.uptimeMillis()
        bind(focusedId = null)
    }

    fun reload() {
        val focused = rows.focusedChild?.tag as? SettingRow
        bind(focused?.id)
    }

    private fun bind(focusedId: String?) {
        val items = SettingCatalog.rows()
        rows.removeAllViews()
        val inflater = LayoutInflater.from(panel.context)
        items.forEach { item ->
            val view = inflater.inflate(R.layout.setting_row, rows, false)
            show(view, item)
            view.setOnClickListener {
                // 打开面板的那次确定键还会落到新获得焦点的行上，忽略它。
                if (SystemClock.uptimeMillis() - openedAt < OPEN_GUARD_MS) return@setOnClickListener
                activate(item)
            }
            rows.addView(view)
        }
        val index = items.indexOfFirst { it.id == focusedId }.coerceAtLeast(0)
        rows.getChildAt(index)?.requestFocus()
    }

    private fun activate(item: SettingRow) {
        when (item) {
            is SettingRow.Handoff -> launch(SettingHandoffs.intent(panel.context, item.target))
            is SettingRow.Hint -> Toast.makeText(panel.context, R.string.source_hint, Toast.LENGTH_LONG).show()
            is SettingRow.Range -> Unit
        }
    }

    private fun show(
        view: View,
        item: SettingRow,
    ) {
        view.tag = item
        view.findViewById<TextView>(R.id.label).setText(label(item.id))
        view.findViewById<TextView>(R.id.value).text =
            when (item) {
                is SettingRow.Handoff -> view.context.getString(R.string.open_page)
                is SettingRow.Hint -> view.context.getString(R.string.source_action)
                is SettingRow.Range -> ""
            }
    }

    private companion object {
        const val OPEN_GUARD_MS = 300L
    }

    private fun label(id: String): Int =
        when (id) {
            SettingCatalog.SOURCE -> R.string.source_signal
            SettingCatalog.PICTURE -> R.string.picture
            SettingCatalog.SOUND_MODE -> R.string.sound_mode
            SettingCatalog.NETWORK -> R.string.network
            SettingCatalog.ANDROID -> R.string.android_settings
            else -> R.string.developer_options
        }
}
