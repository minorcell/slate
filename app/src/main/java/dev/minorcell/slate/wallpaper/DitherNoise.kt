package dev.minorcell.slate.wallpaper

/**
 * Kotlin `Random(seed)` 的 XorWow。壁纸噪点继续用同一串数，画面不会变。
 * 不引用 kotlin.random，那一套会把线程局部随机和区间检查一并打进包里。
 */
internal class DitherNoise(seed: Int) {
    private var x = seed
    private var y = seed shr 31
    private var z = 0
    private var w = 0
    private var v = seed.inv()
    private var addend = (seed shl 10) xor (y ushr 4)

    init {
        var step = 0
        while (step < WARMUP) {
            nextRaw()
            step++
        }
    }

    fun nextInt(
        from: Int,
        until: Int,
    ): Int {
        val span = until - from
        val offset =
            if (span > 0 || span == Int.MIN_VALUE) {
                bounded(span)
            } else {
                var candidate = nextRaw()
                while (candidate < from || candidate >= until) candidate = nextRaw()
                return candidate
            }
        return from + offset
    }

    private fun bounded(span: Int): Int {
        if (span and -span == span) return nextBits(31 - Integer.numberOfLeadingZeros(span))
        var bits: Int
        var value: Int
        do {
            bits = nextRaw() ushr 1
            value = bits % span
        } while (bits - value + (span - 1) < 0)
        return value
    }

    private fun nextBits(bitCount: Int): Int = (nextRaw() ushr (32 - bitCount)) and (-bitCount shr 31)

    private fun nextRaw(): Int {
        var t = x
        t = t xor (t ushr 2)
        x = y
        y = z
        z = w
        val previous = v
        w = previous
        t = t xor (t shl 1) xor previous xor (previous shl 4)
        v = t
        addend += ADDEND_STEP
        return t + addend
    }

    private companion object {
        const val WARMUP = 64
        const val ADDEND_STEP = 362437
    }
}
