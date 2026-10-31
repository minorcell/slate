package dev.minorcell.slate.data.system

import android.content.Intent
import android.provider.Settings
import dev.minorcell.slate.domain.settings.HandoffTarget

/**
 * 没有公开写接口的项，打开系统里已经存在的那一页。
 *
 * [android.provider.Settings.ACTION_SETTINGS] 在这台海信上会被电视设置首页接走，
 * 所以安卓原生设置用 `android.settings.mtk.SETTINGS`。
 */
object SettingHandoffs {
    fun intent(target: HandoffTarget): Intent =
        when (target) {
            HandoffTarget.Picture -> Intent(ACTION_IMAGE)
            HandoffTarget.SoundMode -> Intent(ACTION_TV_SOUND)
            HandoffTarget.Network -> Intent(Settings.ACTION_WIFI_SETTINGS)
            HandoffTarget.AndroidSettings -> Intent(ACTION_ANDROID_SETTINGS)
            HandoffTarget.DeveloperOptions -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private const val ACTION_IMAGE = "android.settings.IMAGE_SETTINGS"
    private const val ACTION_TV_SOUND = "android.settings.TV_SOUND_SETTINGS"
    private const val ACTION_ANDROID_SETTINGS = "android.settings.mtk.SETTINGS"
}
