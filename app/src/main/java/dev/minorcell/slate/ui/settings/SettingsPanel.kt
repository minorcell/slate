package dev.minorcell.slate.ui.settings

import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.minorcell.slate.R
import dev.minorcell.slate.data.system.MediaVolume
import dev.minorcell.slate.data.system.ScreenBrightness
import dev.minorcell.slate.data.system.SettingHandoffs
import dev.minorcell.slate.domain.settings.SettingCatalog
import dev.minorcell.slate.domain.settings.SettingRanges
import dev.minorcell.slate.domain.settings.SettingRow

/**
 * 同一窗口里的设置页。音量和亮度直接改系统值，其余行打开已经存在的系统页。
 */
class SettingsPanel(
    private val panel: View,
    private val volume: MediaVolume,
    private val brightness: ScreenBrightness,
    private val launch: (Intent) -> Unit,
) {
    private val rows = panel.findViewById<LinearLayout>(R.id.settings_rows)

    fun show() {
        panel.visibility = View.VISIBLE
        bind(focusedId = null)
    }

    fun reload() {
        val focused = rows.focusedChild?.tag as? SettingRow
        bind(focused?.id)
    }

    private fun bind(focusedId: String?) {
        val items = SettingCatalog.rows(volume.read(), brightness.read())
        rows.removeAllViews()
        val inflater = LayoutInflater.from(panel.context)
        items.forEach { item ->
            val view = inflater.inflate(R.layout.setting_row, rows, false)
            show(view, item)
            view.setOnKeyListener { row, keyCode, event -> onKey(row, keyCode, event) }
            view.setOnClickListener { activate(view.tag as SettingRow) }
            rows.addView(view)
        }
        val index = items.indexOfFirst { it.id == focusedId }.coerceAtLeast(0)
        rows.getChildAt(index)?.requestFocus()
    }

    private fun onKey(
        view: View,
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val item = view.tag as? SettingRow ?: return false
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                adjust(view, item, -1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                adjust(view, item, 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                activate(item)
                true
            }
            else -> false
        }
    }

    private fun adjust(
        view: View,
        item: SettingRow,
        delta: Int,
    ) {
        val range = item as? SettingRow.Range ?: return
        if (!range.writable) {
            if (range.id == SettingCatalog.BRIGHTNESS) launch(brightness.grantIntent())
            return
        }
        val next = SettingRanges.step(range.value, delta, range.min, range.max, range.stride)
        if (next == range.value) return
        val wrote =
            when (range.id) {
                SettingCatalog.VOLUME -> {
                    volume.write(next)
                    true
                }
                SettingCatalog.BRIGHTNESS -> brightness.write(next)
                else -> false
            }
        if (!wrote) {
            if (range.id == SettingCatalog.BRIGHTNESS) launch(brightness.grantIntent())
            return
        }
        val updated =
            when (range.id) {
                SettingCatalog.VOLUME -> volume.read()
                SettingCatalog.BRIGHTNESS -> brightness.read()
                else -> range.copy(value = next)
            }
        show(view, updated)
    }

    private fun activate(item: SettingRow) {
        when (item) {
            is SettingRow.Handoff -> launch(SettingHandoffs.intent(item.target))
            is SettingRow.Range ->
                if (!item.writable && item.id == SettingCatalog.BRIGHTNESS) {
                    launch(brightness.grantIntent())
                }
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
                is SettingRow.Range -> valueText(item)
                is SettingRow.Handoff -> view.context.getString(R.string.open_page)
            }
    }

    private fun valueText(range: SettingRow.Range): String {
        val context = panel.context
        if (!range.writable) return context.getString(R.string.allow_brightness)
        return if (range.id == SettingCatalog.BRIGHTNESS) {
            val percent = range.value * PERCENT / range.max.coerceAtLeast(1)
            context.getString(R.string.percent, percent)
        } else {
            context.getString(R.string.level, range.value, range.max)
        }
    }

    private fun label(id: String): Int =
        when (id) {
            SettingCatalog.VOLUME -> R.string.volume
            SettingCatalog.BRIGHTNESS -> R.string.brightness
            SettingCatalog.PICTURE -> R.string.picture
            SettingCatalog.SOUND_MODE -> R.string.sound_mode
            SettingCatalog.NETWORK -> R.string.network
            SettingCatalog.ANDROID -> R.string.android_settings
            else -> R.string.developer_options
        }

    private companion object {
        const val PERCENT = 100
    }
}
