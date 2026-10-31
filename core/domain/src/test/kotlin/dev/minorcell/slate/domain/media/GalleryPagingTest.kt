package dev.minorcell.slate.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryPagingTest {
    @Test
    fun `moves one step inside the range`() {
        assertEquals(2, GalleryPaging.page(current = 1, delta = 1, count = 5))
        assertEquals(0, GalleryPaging.page(current = 1, delta = -1, count = 5))
    }

    @Test
    fun `stops at both ends`() {
        assertEquals(0, GalleryPaging.page(current = 0, delta = -1, count = 5))
        assertEquals(4, GalleryPaging.page(current = 4, delta = 1, count = 5))
    }

    @Test
    fun `clamps an out of range jump`() {
        assertEquals(4, GalleryPaging.page(current = 3, delta = 9, count = 5))
    }

    @Test
    fun `empty album stays at zero`() {
        assertEquals(0, GalleryPaging.page(current = 3, delta = 1, count = 0))
    }
}
