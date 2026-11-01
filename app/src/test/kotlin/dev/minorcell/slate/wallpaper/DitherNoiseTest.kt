package dev.minorcell.slate.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class DitherNoiseTest {
    @Test
    fun matchesKotlinRandom() {
        val expected = Random(7)
        val actual = DitherNoise(7)
        repeat(2000) {
            assertEquals(expected.nextInt(-2, 3), actual.nextInt(-2, 3))
        }
    }
}
