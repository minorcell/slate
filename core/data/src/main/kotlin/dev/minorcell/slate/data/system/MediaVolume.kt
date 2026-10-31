package dev.minorcell.slate.data.system

import android.content.Context
import android.media.AudioManager
import dev.minorcell.slate.domain.settings.SettingCatalog
import dev.minorcell.slate.domain.settings.SettingRow

/**
 * 媒体音量。遥控器的音量加、音量减改的就是这条流，不需要打开设置。
 */
class MediaVolume(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun read(): SettingRow.Range {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val value = audio.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
        return SettingRow.Range(SettingCatalog.VOLUME, value, 0, max, STRIDE, true)
    }

    fun write(value: Int) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, value.coerceIn(0, max), 0)
    }

    private companion object {
        const val STRIDE = 1
    }
}
