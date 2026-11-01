package dev.minorcell.slate.ui.theme

import android.graphics.Typeface
import android.os.Build

/**
 * 系统无衬线。时钟更重，说明更轻。
 * Android 9 以下没有可变字重，600 及以上退回 sans-serif-medium。
 * 字距按电视大字的用法：时钟收紧，标题略松。
 */
object SlateType {
    fun clock(): Typeface = face(WEIGHT_CLOCK)

    fun date(): Typeface = face(WEIGHT_DATE)

    fun title(): Typeface = face(WEIGHT_TITLE)

    fun hint(): Typeface = face(WEIGHT_HINT)

    fun shelf(): Typeface = face(WEIGHT_SHELF)

    private fun face(weight: Int): Typeface =
        cache.getOrPut(weight) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(Typeface.SANS_SERIF, weight, false)
            } else if (weight >= MEDIUM_FROM) {
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
            } else {
                Typeface.SANS_SERIF
            }
        }

    private val cache = HashMap<Int, Typeface>()

    const val CLOCK_TRACKING = -0.04f
    const val TITLE_TRACKING = -0.015f

    private const val WEIGHT_CLOCK = 640
    private const val WEIGHT_DATE = 560
    private const val WEIGHT_TITLE = 600
    private const val WEIGHT_HINT = 500
    private const val WEIGHT_SHELF = 560
    private const val MEDIUM_FROM = 600
}
