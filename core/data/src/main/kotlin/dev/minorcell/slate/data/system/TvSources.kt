package dev.minorcell.slate.data.system

import android.content.Context
import android.media.tv.TvInputManager

data class TvSource(
    val id: String,
    val label: String,
)

class TvSources(private val context: Context) {
    fun load(): List<TvSource> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return manager.tvInputList.mapNotNull { info ->
            val label = info.loadLabel(context)?.toString()?.trim().orEmpty()
            if (label.isEmpty()) null else TvSource(info.id, label)
        }
    }
}
