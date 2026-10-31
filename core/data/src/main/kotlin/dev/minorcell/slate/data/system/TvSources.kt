package dev.minorcell.slate.data.system

import android.content.Context
import android.content.Intent
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build

enum class SourceKind { TUNER, HDMI, AV, OTHER }

data class TvSource(
    val id: String,
    val label: String,
    val kind: SourceKind = SourceKind.OTHER,
)

class TvSources(private val context: Context) {
    private val manager: TvInputManager?
        get() = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager

    fun load(): List<TvSource> {
        val manager = manager ?: return emptyList()
        return manager.tvInputList.mapNotNull { info ->
            val label = info.loadLabel(context)?.toString()?.trim().orEmpty()
            if (label.isEmpty()) null else TvSource(info.id, label, kindOf(info.type))
        }
    }

    /**
     * TvInputInfo 只提供 setup 入口（API 26+），框架没有公开的切换输入接口。
     * 有就打开系统自己那一页，没有就什么也不做，不弹假提示。
     */
    fun setupIntent(inputId: String): Intent? {
        if (Build.VERSION.SDK_INT < 26) return null
        val info = manager?.getTvInputInfo(inputId) ?: return null
        return info.createSetupIntent()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun kindOf(type: Int): SourceKind =
        when (type) {
            TvInputInfo.TYPE_TUNER -> SourceKind.TUNER
            TvInputInfo.TYPE_HDMI -> SourceKind.HDMI
            TvInputInfo.TYPE_COMPOSITE, TvInputInfo.TYPE_SVIDEO, TvInputInfo.TYPE_SCART -> SourceKind.AV
            else -> SourceKind.OTHER
        }
}
