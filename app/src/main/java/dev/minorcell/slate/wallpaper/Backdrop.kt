package dev.minorcell.slate.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * 首页背景。按窗口像素画一张夜色山丘，再烤一张小的磨砂图给底板用。
 * 磨砂只做一次：这块电视是 Android 9，没有每帧的实时模糊，实时模糊也撑不住。
 */
object Backdrop {
    data class Picture(val sharp: Bitmap, val frost: Bitmap)

    fun render(
        width: Int,
        height: Int,
    ): Picture {
        val sharp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sharp)
        paintScene(canvas, width.toFloat(), height.toFloat())
        dither(sharp)
        return Picture(sharp, frost(sharp))
    }

    private fun paintScene(
        canvas: Canvas,
        width: Float,
        height: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        sky(canvas, paint, width, height)
        moon(canvas, paint, width, height)
        hill(canvas, paint, width, height, Ridge(0.62f, 0.50f, HILL_FAR_TOP, HILL_FAR_BOTTOM))
        hill(canvas, paint, width, height, Ridge(0.74f, 0.62f, HILL_MID_TOP, HILL_MID_BOTTOM))
        hill(canvas, paint, width, height, Ridge(0.86f, 0.76f, HILL_NEAR_TOP, HILL_NEAR_BOTTOM))
        shade(canvas, paint, width, height)
    }

    private fun sky(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float,
    ) {
        paint.shader =
            LinearGradient(
                0f,
                0f,
                0f,
                height * SKY_END,
                intArrayOf(SKY_TOP, SKY_MID, SKY_HORIZON),
                floatArrayOf(0f, SKY_MID_STOP, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    private fun moon(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float,
    ) {
        val cx = width * MOON_X
        val cy = height * MOON_Y
        val radius = width * MOON_RADIUS
        paint.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(MOON_CORE, MOON_HALO, MOON_EDGE),
                floatArrayOf(0f, MOON_HALO_STOP, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    private fun hill(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float,
        ridge: Ridge,
    ) {
        val crest = height * ridge.peak
        val foot = height * ridge.baseline
        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(0f, foot)
        path.quadTo(width * HILL_CREST_X, crest, width, foot + height * HILL_RIGHT_DROP)
        path.lineTo(width, height)
        path.close()
        paint.shader = LinearGradient(0f, crest, 0f, height, ridge.top, ridge.bottom, Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    private fun shade(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float,
    ) {
        paint.shader =
            LinearGradient(
                0f,
                0f,
                width * SHADE_REACH,
                0f,
                SHADE,
                SHADE_CLEAR,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    private fun dither(bitmap: Bitmap) {
        val width = bitmap.width
        val row = IntArray(width)
        val random = DitherNoise(DITHER_SEED)
        var y = 0
        while (y < bitmap.height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var x = 0
            while (x < row.size) {
                val color = row[x]
                val noise = random.nextInt(DITHER_MIN, DITHER_MAX)
                val red = ((color shr RED_SHIFT) and BYTE_MASK) + noise
                val green = ((color shr GREEN_SHIFT) and BYTE_MASK) + noise
                val blue = (color and BYTE_MASK) + noise
                row[x] =
                    (color and ALPHA_MASK) or
                    (clamp(red, 0, BYTE_MASK) shl RED_SHIFT) or
                    (clamp(green, 0, BYTE_MASK) shl GREEN_SHIFT) or
                    clamp(blue, 0, BYTE_MASK)
                x++
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
            y++
        }
    }

    private fun frost(source: Bitmap): Bitmap {
        val width = FROST_WIDTH
        val height = FROST_WIDTH * source.height / source.width
        val small = Bitmap.createScaledBitmap(source, width, atLeast(height, 1), true)
        val pixels = IntArray(small.width * small.height)
        small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        var pass = 0
        while (pass < BLUR_PASSES) {
            blur(pixels, small.width, small.height, BLUR_RADIUS)
            pass++
        }
        small.setPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        return small
    }

    private fun blur(
        pixels: IntArray,
        width: Int,
        height: Int,
        radius: Int,
    ) {
        val tmp = IntArray(pixels.size)
        blurAxis(pixels, tmp, width, height, radius, horizontal = true)
        blurAxis(tmp, pixels, width, height, radius, horizontal = false)
    }

    private fun blurAxis(
        source: IntArray,
        dest: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        horizontal: Boolean,
    ) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val window = radius * 2 + 1
        var line = 0
        while (line < lines) {
            var index = 0
            while (index < length) {
                var alpha = 0
                var red = 0
                var green = 0
                var blue = 0
                var offset = -radius
                while (offset <= radius) {
                    val sample = clamp(index + offset, 0, length - 1)
                    val color = if (horizontal) source[line * width + sample] else source[sample * width + line]
                    alpha += color ushr ALPHA_SHIFT
                    red += (color shr RED_SHIFT) and BYTE_MASK
                    green += (color shr GREEN_SHIFT) and BYTE_MASK
                    blue += color and BYTE_MASK
                    offset++
                }
                val at = if (horizontal) line * width + index else index * width + line
                dest[at] =
                    ((alpha / window) shl ALPHA_SHIFT) or
                    ((red / window) shl RED_SHIFT) or
                    ((green / window) shl GREEN_SHIFT) or
                    (blue / window)
                index++
            }
            line++
        }
    }

    private fun clamp(
        value: Int,
        min: Int,
        max: Int,
    ): Int {
        if (value < min) return min
        if (value > max) return max
        return value
    }

    private fun atLeast(
        value: Int,
        min: Int,
    ): Int = if (value < min) min else value

    private const val SKY_TOP = 0xFF07101C.toInt()
    private const val SKY_MID = 0xFF243656.toInt()
    private const val SKY_HORIZON = 0xFF7A6278.toInt()
    private const val SKY_END = 0.68f
    private const val SKY_MID_STOP = 0.55f
    private const val MOON_CORE = 0xF2F4F7FF.toInt()
    private const val MOON_HALO = 0x66C9D4F2.toInt()
    private const val MOON_EDGE = 0x00C9D4F2
    private const val MOON_X = 0.78f
    private const val MOON_Y = 0.20f
    private const val MOON_RADIUS = 0.28f
    private const val MOON_HALO_STOP = 0.18f
    private const val HILL_FAR_TOP = 0xFF4A6280.toInt()
    private const val HILL_FAR_BOTTOM = 0xFF2C415C.toInt()
    private const val HILL_MID_TOP = 0xFF2E4868.toInt()
    private const val HILL_MID_BOTTOM = 0xFF1A2C44.toInt()
    private const val HILL_NEAR_TOP = 0xFF1A304C.toInt()
    private const val HILL_NEAR_BOTTOM = 0xFF0C1624.toInt()
    private const val HILL_CREST_X = 0.46f
    private const val HILL_RIGHT_DROP = 0.04f
    private const val SHADE = 0x8C05080E.toInt()
    private const val SHADE_CLEAR = 0x0005080E
    private const val SHADE_REACH = 0.62f
    private const val FROST_WIDTH = 320
    private const val BLUR_PASSES = 3
    private const val BLUR_RADIUS = 8
    private const val DITHER_SEED = 7
    private const val DITHER_MIN = -2
    private const val DITHER_MAX = 3
    private const val BYTE_MASK = 0xFF
    private const val ALPHA_MASK = 0xFF000000.toInt()
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8

    private class Ridge(
        val baseline: Float,
        val peak: Float,
        val top: Int,
        val bottom: Int,
    )
}
