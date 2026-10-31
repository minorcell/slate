package dev.minorcell.slate.domain.settings

/**
 * 设置页的一行。数值行由系统接口读写，跳转行只描述要打开的系统页。
 */
sealed interface SettingRow {
    val id: String

    data class Range(
        override val id: String,
        val value: Int,
        val min: Int,
        val max: Int,
        val stride: Int,
        val writable: Boolean,
    ) : SettingRow

    data class Handoff(
        override val id: String,
        val target: HandoffTarget,
    ) : SettingRow
}

enum class HandoffTarget {
    Picture,
    SoundMode,
    Network,
    AndroidSettings,
    DeveloperOptions,
}

object SettingRanges {
    fun step(
        value: Int,
        delta: Int,
        min: Int,
        max: Int,
        stride: Int,
    ): Int {
        if (stride <= 0 || max < min) return value.coerceIn(min, max)
        return (value + delta * stride).coerceIn(min, max)
    }
}

object SettingCatalog {
    const val VOLUME = "volume"
    const val BRIGHTNESS = "brightness"
    const val PICTURE = "picture"
    const val SOUND_MODE = "sound_mode"
    const val NETWORK = "network"
    const val ANDROID = "android"
    const val DEVELOPER = "developer"

    fun rows(
        volume: SettingRow.Range,
        brightness: SettingRow.Range,
    ): List<SettingRow> =
        listOf(
            volume,
            brightness,
            SettingRow.Handoff(PICTURE, HandoffTarget.Picture),
            SettingRow.Handoff(SOUND_MODE, HandoffTarget.SoundMode),
            SettingRow.Handoff(NETWORK, HandoffTarget.Network),
            SettingRow.Handoff(ANDROID, HandoffTarget.AndroidSettings),
            SettingRow.Handoff(DEVELOPER, HandoffTarget.DeveloperOptions),
        )
}
