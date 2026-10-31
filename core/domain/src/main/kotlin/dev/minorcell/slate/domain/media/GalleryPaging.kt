package dev.minorcell.slate.domain.media

/** 相册大图翻页：到两端停住，不循环。 */
object GalleryPaging {
    fun page(
        current: Int,
        delta: Int,
        count: Int,
    ): Int {
        if (count <= 0) return 0
        return (current + delta).coerceIn(0, count - 1)
    }
}
