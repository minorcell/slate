package dev.minorcell.slate.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.os.Build

/**
 * Inter，和 Aerial 同一套可变字体。大字用 Display 光学尺寸，小字用 Text。
 * 变体轴要 Android 8.0；更早的系统没有这个轴，退回系统无衬线。
 * 字重和字距按 tvOS 的用法：时钟收紧，说明字略松。
 */
object SlateType {
    fun clock(context: Context): Typeface = face(context, WEIGHT_CLOCK, OPSZ_DISPLAY)

    fun date(context: Context): Typeface = face(context, WEIGHT_DATE, OPSZ_DISPLAY)

    fun title(context: Context): Typeface = face(context, WEIGHT_TITLE, OPSZ_DISPLAY)

    fun hint(context: Context): Typeface = face(context, WEIGHT_HINT, OPSZ_TEXT)

    fun shelf(context: Context): Typeface = face(context, WEIGHT_SHELF, OPSZ_TEXT)

    private fun face(
        context: Context,
        weight: Int,
        opticalSize: Int,
    ): Typeface {
        if (Build.VERSION.SDK_INT < VARIATION_SDK) return Typeface.SANS_SERIF
        val key = weight * WEIGHT_KEY + opticalSize
        return cache.getOrPut(key) {
            Typeface.Builder(context.assets, FONT)
                .setFontVariationSettings("'$AXIS_WEIGHT' $weight, '$AXIS_OPTICAL' $opticalSize")
                .build()
        }
    }

    private val cache = HashMap<Int, Typeface>()

    const val CLOCK_TRACKING = -0.04f
    const val TITLE_TRACKING = -0.015f

    private const val FONT = "fonts/InterVariable.ttf"
    private const val VARIATION_SDK = 26
    private const val AXIS_WEIGHT = "wght"
    private const val AXIS_OPTICAL = "opsz"
    private const val OPSZ_DISPLAY = 32
    private const val OPSZ_TEXT = 14
    private const val WEIGHT_CLOCK = 640
    private const val WEIGHT_DATE = 560
    private const val WEIGHT_TITLE = 600
    private const val WEIGHT_HINT = 500
    private const val WEIGHT_SHELF = 560
    private const val WEIGHT_KEY = 100
}
