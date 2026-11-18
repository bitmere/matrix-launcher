package com.matrix.launcher

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class MatrixLayoutTest {
    @Test fun randomizedTvWidthsKeepWholeSlotsAndRevealConverges() {
        val random = kotlin.random.Random(5301)
        repeat(10_000) {
            val viewport = random.nextInt(640, 3841)
            val inset = random.nextInt(4, 25)
            val gap = random.nextInt(8, 33)
            val fit = MatrixLayout.rowFit(viewport, random.nextInt(60, 361), inset, gap,
                if (random.nextBoolean()) random.nextInt(3, 7) else null)
            assertTrue(fit.viewport <= viewport)
            assertEquals(fit.viewport, fit.cardWidth * fit.columns + fit.gap * (fit.columns - 1) + inset * 2)
            val offset = random.nextInt(-viewport, viewport * 2).toFloat()
            val distance = ComposeRevealPolicy.distance(offset, fit.cardWidth.toFloat(), fit.viewport.toFloat(), inset.toFloat())
            assertEquals(0f, ComposeRevealPolicy.distance(offset - distance, fit.cardWidth.toFloat(), fit.viewport.toFloat(), inset.toFloat()), .001f)
        }
    }
    @Test fun outwardGlowAndScaleFitInsideTheSharedFocusGutter() {
        val outside = MatrixLayout.focusOutlineOffset + MatrixLayout.focusOutlineWidth / 2 + MatrixLayout.focusHaloSpread
        assertTrue(MatrixLayout.focusOutlineOffset - MatrixLayout.focusOutlineWidth / 2 >= 0.dp)
        assertTrue(outside <= MatrixLayout.artworkInset)
        // Largest current 3-column artwork at the standard 960dp TV viewport.
        val width = MatrixLayout.cardWidth(960.dp, 3, MatrixLayout.channelInset)
        val paintedHalfWidth = width / 2 - MatrixLayout.artworkInset + outside
        val overhang = paintedHalfWidth * MatrixLayout.cardFocusScale - width / 2
        assertTrue(overhang < MatrixLayout.focusInset)
        // Reserving glow space must still leave usable artwork at the smallest supported TV size.
        assertTrue(MatrixLayout.cardWidth(640.dp, 6, MatrixLayout.channelInset) - MatrixLayout.artworkInset * 2 > 48.dp)
    }
    @Test fun portraitListsFitAcrossTheViewportAtEveryUserSize() {
        val viewport = 900.dp
        for (columns in 3..6) {
            val landscape = MatrixLayout.cardWidth(viewport, columns, 10.dp)
            val poster = MatrixLayout.mediaWidth(landscape, true)
            val count = MatrixLayout.fittingColumns(viewport, poster, 10.dp)
            val used = poster * count + MatrixLayout.gap * (count - 1) + 20.dp
            assertTrue("Posters must fit without truncating the last column", used <= viewport)
            assertTrue("Portraits should not consume landscape widths", poster < landscape)
            assertTrue(poster <= 144.dp)
            assertEquals(landscape, MatrixLayout.mediaWidth(landscape, false))
        }
    }
    @Test fun dpadRevealCannotEatTheFixedRowMargin() {
        for (inset in listOf(MatrixLayout.channelInset, MatrixLayout.dockInset)) {
            val outer = inset - MatrixLayout.focusInset
            val cardOffset = MatrixLayout.focusInset.value
            assertEquals(0f, ComposeRevealPolicy.distance(cardOffset, 160f, 500f, cardOffset), 0f)
            assertEquals(inset.value, outer.value + cardOffset, 0f)
        }
    }
    @Test fun completeSlotsFitAtDifferentDensitiesAndPortraitSizes() {
        for (viewport in listOf(640, 855, 960, 1280, 1719, 1920, 2560)) {
            for (desired in listOf(80, 97, 144, 260)) {
                val fit = MatrixLayout.rowFit(viewport, desired, 8, 16)
                assertTrue(fit.columns in 1..10)
                assertTrue(fit.viewport <= viewport)
                assertEquals(desired, fit.cardWidth)
                assertTrue(fit.gap >= 16)
                assertEquals(fit.viewport, fit.columns * fit.cardWidth + (fit.columns - 1) * fit.gap + 16)
                // Revealing the next slot moves a whole stride, not part of a neighboring card.
                val nextOffset = 8f + fit.columns * (fit.cardWidth + fit.gap)
                assertEquals((fit.cardWidth + fit.gap).toFloat(),
                    ComposeRevealPolicy.distance(nextOffset, fit.cardWidth.toFloat(), fit.viewport.toFloat(), 8f), .001f)
                assertTrue(nextOffset >= fit.viewport)
            }
        }
    }
    @Test fun largePortraitsKeepTheirExistingCapInsteadOfGrowingToFillTheRow() {
        val fit = MatrixLayout.rowFit(872, 144, 8, 16)
        assertEquals(5, fit.columns)
        assertEquals(144, fit.cardWidth)
        assertEquals(34, fit.gap)
        assertEquals(872, fit.viewport)
    }
    @Test fun dockCapacityDoesNotLimitTheTotalFavoriteCount() {
        val fit = MatrixLayout.rowFit(1720, 312, 16, 32, 5)
        assertEquals(5, fit.columns)
        val sixth = 16f + 5 * (fit.cardWidth + 32)
        assertTrue(sixth > fit.viewport)
        assertEquals((fit.cardWidth + 32).toFloat(), ComposeRevealPolicy.distance(sixth,
            fit.cardWidth.toFloat(), fit.viewport.toFloat(), 16f), .001f)
    }
}
