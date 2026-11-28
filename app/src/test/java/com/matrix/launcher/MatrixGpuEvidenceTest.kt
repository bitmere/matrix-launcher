package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class MatrixGpuEvidenceTest {
    private fun baseline() = MatrixGpuEvidence().apply {
        repeat(MatrixGpuEvidence.baselineCount) { frame(7_000_000, 3_000_000, 0) }
    }
    @Test fun emptyEvidenceCannotPromote() {
        val evidence = MatrixGpuEvidence()
        assertFalse(evidence.qualified)
        assertEquals(MatrixMaterialQuality.STATIC, MatrixCapabilities.decide(false, false, 37,
            measuredGpu = evidence.qualified).material)
    }
    @Test fun measuredCompletionPromotesAndFailureRevokes() {
        val evidence = baseline()
        assertFalse(evidence.qualified)
        repeat(MatrixGpuEvidence.probeCount) { assertTrue(evidence.probe(3_000_000)) }
        assertEquals(MatrixMaterialQuality.FULL, MatrixCapabilities.decide(false, false, 33,
            measuredGpu = evidence.qualified).material)
        evidence.fail()
        assertEquals(MatrixMaterialQuality.STATIC, MatrixCapabilities.decide(false, false, 33,
            measuredGpu = evidence.qualified).material)
    }
    @Test fun probeWithoutBaselineCannotQualify() {
        val evidence = MatrixGpuEvidence()
        repeat(24) { assertFalse(evidence.probe(1_000_000)) }
        assertFalse(evidence.qualified)
    }
    @Test fun missingGpuMetricDroppedOrSlowFramesCloseGate() {
        for ((total, gpu, dropped) in listOf(Triple(1L, -1L, 0), Triple(1L, 0L, 0),
            Triple(8_000_001L, 1L, 0), Triple(1L, 8_000_001L, 0), Triple(1L, 1L, 1))) {
            val evidence = MatrixGpuEvidence()
            evidence.frame(total, gpu, dropped)
            repeat(120) { evidence.frame(1, 1, 0) }
            assertFalse(evidence.baselineReady)
        }
    }
    @Test fun incompleteProbeStaysStatic() {
        val evidence = baseline()
        repeat(23) { evidence.probe(1) }
        assertFalse(evidence.qualified)
    }
    @Test fun badProbePermanentlyInvalidatesWholeRun() {
        for (bad in listOf(-1L, 0L, 4_000_001L)) {
            val evidence = baseline()
            repeat(12) { evidence.probe(1) }
            assertFalse(evidence.probe(bad))
            repeat(24) { evidence.probe(1) }
            assertFalse(evidence.qualified)
        }
    }
    @Test fun thresholdsAreInclusiveAndSafetyOverridesEvidence() {
        val evidence = MatrixGpuEvidence()
        repeat(120) { evidence.frame(8_000_000, 8_000_000, 0) }
        repeat(24) { evidence.probe(4_000_000) }
        assertTrue(evidence.qualified)
        assertEquals(MatrixMaterialQuality.CHEAP, MatrixCapabilities.decide(true, false, 33,
            measuredGpu = evidence.qualified).material)
        assertEquals(MatrixMaterialQuality.CHEAP, MatrixCapabilities.decide(false, true, 33,
            measuredGpu = evidence.qualified).material)
    }
}
