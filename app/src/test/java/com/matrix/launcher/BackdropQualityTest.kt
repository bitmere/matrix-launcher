package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class BackdropQualityTest {
    @Test fun fullHdAnd4kUse720pTexturesWithoutStretchingOrUnboundedMemory() {
        assertEquals(TextureSize(1280, 720), BackdropQuality.size(1920, 1080))
        assertEquals(TextureSize(1280, 720), BackdropQuality.size(3840, 2160))
        for ((w, h) in listOf(1920 to 1080, 880 to 1080, 640 to 360)) {
            val size = BackdropQuality.size(w, h)
            assertTrue(size.width <= 1280 && size.height <= 720)
            assertEquals(w.toFloat()/h, size.width.toFloat()/size.height, .002f)
            assertTrue(size.width * size.height * 8 <= BackdropQuality.cacheBytes)
        }
    }
}
