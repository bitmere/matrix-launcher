package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class ArtworkFitTest {
    @Test fun variableHeightPostersDecorateImageInsteadOfLetterboxSpace() {
        val fit = ArtworkFit.bounds(200f, 300f, .8f)
        assertEquals(200f, fit.width, .001f)
        assertEquals(250f, fit.height, .001f)
        val narrow = ArtworkFit.bounds(200f, 300f, .5f)
        assertEquals(150f, narrow.width, .001f)
        assertEquals(300f, narrow.height, .001f)
    }
    @Test fun fittedBoundsPreserveSourceAndNeverCropOrOverflow() {
        for (aspect in listOf(.5f, 2f/3, .8f, 1f, 16f/9, 2.4f)) {
            val fit = ArtworkFit.bounds(240f, 360f, aspect)
            assertTrue(fit.width <= 240f && fit.height <= 360f)
            assertEquals(aspect, fit.width / fit.height, .001f)
        }
        assertEquals(2f/3, ArtworkFit.aspect(0, 0, 2f/3), 0f)
        assertEquals(.8f, ArtworkFit.aspect(400, 500, 1f), 0f)
        assertEquals(ArtworkBounds(0f, 0f), ArtworkFit.bounds(0f, 300f, Float.NaN))
    }
    @Test fun appPaletteHasOpaqueDistinctCompanionWithoutChangingTheAccent() {
        val palette = ArtworkPalette.app(0xffcc2200.toInt())
        assertEquals(0xffcc2200.toInt(), palette.primary)
        assertEquals(255, palette.secondary ushr 24)
        assertNotEquals(palette.primary, palette.secondary)
        assertEquals(palette, ArtworkPalette.app(palette.primary))
    }
}
