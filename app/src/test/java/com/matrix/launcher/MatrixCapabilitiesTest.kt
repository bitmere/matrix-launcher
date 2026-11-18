package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixCapabilitiesTest {

    @Test
    fun fullEffectsWhenNeitherSafeNorLowRam() {
        assertEquals(
            MatrixCapabilities.FULL,
            MatrixCapabilities.decide(safeMode = false, lowRam = false)
        )
    }

    @Test
    fun cheapVariantWhenSafeMode() {
        assertEquals(
            MatrixCapabilities.CHEAP,
            MatrixCapabilities.decide(safeMode = true, lowRam = false)
        )
    }

    @Test
    fun cheapVariantWhenLowRam() {
        assertEquals(
            MatrixCapabilities.CHEAP,
            MatrixCapabilities.decide(safeMode = false, lowRam = true)
        )
    }

    @Test
    fun cheapVariantWhenBoth() {
        assertEquals(
            MatrixCapabilities.CHEAP,
            MatrixCapabilities.decide(safeMode = true, lowRam = true)
        )
    }
}
