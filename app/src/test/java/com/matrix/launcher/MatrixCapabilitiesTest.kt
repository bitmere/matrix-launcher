package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class MatrixCapabilitiesTest {
    @Test fun unmeasuredGpuDefaultsToStatic() {
        assertEquals(MatrixCapabilities.STATIC, MatrixCapabilities.decide(false, false, 37))
    }
    @Test fun measuredModernGpuGetsFull() {
        assertEquals(MatrixCapabilities.FULL, MatrixCapabilities.decide(false, false, 31, measuredGpu = true))
    }
    @Test fun measuredGpuGetsFullOnApi31And32() {
        // Locks in the API 31/32 enablement: the GPU qualification may now
        // produce measuredGpu on 31/32 (fence-less probe), so decide() must
        // grant FULL there, not just on 33+.
        assertEquals(MatrixCapabilities.FULL, MatrixCapabilities.decide(false, false, 31, measuredGpu = true))
        assertEquals(MatrixCapabilities.FULL, MatrixCapabilities.decide(false, false, 32, measuredGpu = true))
    }
    @Test fun cheapVariantWhenSafeMode() {
        assertEquals(MatrixCapabilities.CHEAP, MatrixCapabilities.decide(true, false, 31))
    }
    @Test fun cheapVariantWhenLowRam() {
        assertEquals(MatrixCapabilities.CHEAP, MatrixCapabilities.decide(false, true, 31))
    }
    @Test fun cheapVariantWhenBoth() {
        assertEquals(MatrixCapabilities.CHEAP, MatrixCapabilities.decide(true, true, 31))
    }
    @Test fun liveBlurEligibilityMatrixRetainsOldSdkAndMemoryCoverage() {
        for (sdk in listOf(26, 30, 31, 32, 34, 37)) for (safe in listOf(false, true))
            for (ram in listOf(false, true)) for (measured in listOf(false, true)) {
                val value = MatrixCapabilities.decide(safe, ram, sdk, measured)
                assertEquals("sdk=$sdk safe=$safe lowRam=$ram measured=$measured",
                    sdk >= 31 && measured && !safe && !ram, value.liveBlur)
                assertEquals(!safe && !ram, value.wallpaper)
                assertEquals(value.liveBlur, value.animations)
                assertEquals(!safe && !ram, value.transitions)
            }
    }
    @Test fun failureAndUserLowEffectsOverrideMeasuredGpu() {
        assertEquals(MatrixCapabilities.CHEAP, MatrixCapabilities.decide(false, false, 37, true, allocationFailed = true))
        assertEquals(MatrixCapabilities.CHEAP, MatrixCapabilities.decide(false, false, 37, true, lowEffects = true))
    }
    @Test fun motionAndWindowSupportAreIndependentOfMaterial() {
        val value = MatrixCapabilities.decide(false, false, 31, true, reducedMotion = true)
        assertTrue(value.liveBlur); assertFalse(value.transitions); assertFalse(value.animations)
        assertFalse(value.crossWindowBlur)
        val static = MatrixCapabilities.decide(false, false, 31, crossWindowSupported = true)
        assertTrue(static.crossWindowBlur); assertFalse(static.liveBlur)
        assertFalse(MatrixCapabilities.decide(false, false, 30, crossWindowSupported = true).crossWindowBlur)
    }
}
