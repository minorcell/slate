package dev.minorcell.slate.data.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dev.minorcell.slate.domain.settings.HandoffTarget

/**
 * 没有公开写接口的项，打开系统里已经存在的那一页。
 *
 * 图像、声音是厂商页面，不是每台电视都有；按顺序找第一个能接住的，
 * 都不行就落到系统设置首页，避免直接弹「无法打开」。
 */
object SettingHandoffs {
    fun intent(
        context: Context,
        target: HandoffTarget,
    ): Intent {
        val candidates =
            when (target) {
                HandoffTarget.Picture ->
                    listOf(Intent(ACTION_IMAGE), Intent(ACTION_ANDROID_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                HandoffTarget.SoundMode ->
                    listOf(Intent(ACTION_TV_SOUND), Intent(ACTION_SOUND), Intent(Settings.ACTION_SETTINGS))
                HandoffTarget.Network ->
                    listOf(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                HandoffTarget.AndroidSettings ->
                    listOf(Intent(ACTION_ANDROID_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                HandoffTarget.DeveloperOptions ->
                    listOf(
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
                        Intent(Settings.ACTION_SETTINGS),
                    )
            }
        val chosen = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: candidates.last()
        return chosen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private const val ACTION_IMAGE = "android.settings.IMAGE_SETTINGS"
    private const val ACTION_TV_SOUND = "android.settings.TV_SOUND_SETTINGS"
    private const val ACTION_SOUND = "android.settings.SOUND_SETTINGS"
    private const val ACTION_ANDROID_SETTINGS = "android.settings.mtk.SETTINGS"
}
