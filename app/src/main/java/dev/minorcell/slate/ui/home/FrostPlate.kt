package dev.minorcell.slate.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View

/**
 * 底板。取磨砂壁纸里自己身后的那一块铺上，再压一层暗色，白字才站得住。
 * 壁纸是事先烤好的，这里只做一次填充。
 */
class FrostPlate(
    private val frost: Bitmap,
    private val radius: Float,
) : Drawable() {
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veil = Paint().apply { color = VEIL }
    private val rim =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = RIM_WIDTH
            color = RIM
        }
    private val path = Path()
    private val dest = RectF()
    private val source = Rect()
    private var frameLeft = 0
    private var frameTop = 0
    private var frameWidth = 1
    private var frameHeight = 1
    private var spaceWidth = 1
    private var spaceHeight = 1

    @Suppress("LongParameterList")
    fun place(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        spaceW: Int,
        spaceH: Int,
    ) {
        frameLeft = left
        frameTop = top
        frameWidth = atLeast(width, 1)
        frameHeight = atLeast(height, 1)
        spaceWidth = atLeast(spaceW, 1)
        spaceHeight = atLeast(spaceH, 1)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        dest.set(bounds)
        val scaleX = frost.width.toFloat() / spaceWidth
        val scaleY = frost.height.toFloat() / spaceHeight
        source.set(
            (frameLeft * scaleX).toInt(),
            (frameTop * scaleY).toInt(),
            atLeast(((frameLeft + frameWidth) * scaleX).toInt(), 1),
            atLeast(((frameTop + frameHeight) * scaleY).toInt(), 1),
        )
        path.reset()
        path.addRoundRect(dest, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(frost, source, dest, bitmapPaint)
        canvas.drawRect(dest, veil)
        canvas.restore()
        val inset = rim.strokeWidth / 2f
        canvas.drawRoundRect(
            dest.left + inset,
            dest.top + inset,
            dest.right - inset,
            dest.bottom - inset,
            radius,
            radius,
            rim,
        )
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun atLeast(
        value: Int,
        min: Int,
    ): Int = if (value < min) min else value

    companion object {
        private const val VEIL = 0x66101828
        private const val RIM = 0x59FFFFFF
        private const val RIM_WIDTH = 1.5f

        fun track(
            plate: View,
            backdrop: View,
            frost: Bitmap,
            radius: Float,
        ) {
            val drawable = FrostPlate(frost, radius)
            plate.background = drawable
            val sync = {
                val plateAt = IntArray(2)
                val backdropAt = IntArray(2)
                plate.getLocationInWindow(plateAt)
                backdrop.getLocationInWindow(backdropAt)
                drawable.place(
                    plateAt[0] - backdropAt[0],
                    plateAt[1] - backdropAt[1],
                    plate.width,
                    plate.height,
                    backdrop.width,
                    backdrop.height,
                )
            }
            plate.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> sync() }
            sync()
        }
    }
}
