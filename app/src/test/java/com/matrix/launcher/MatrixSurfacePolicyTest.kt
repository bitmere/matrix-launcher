package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class MatrixSurfacePolicyTest {
    @Test fun headerSelectsLiveFrostOrSolid() {
        assertEquals(HeaderTreatment.LIVE, headerTreatment(MatrixMaterialQuality.FULL))
        assertEquals(HeaderTreatment.FROST, headerTreatment(MatrixMaterialQuality.STATIC))
        assertEquals(HeaderTreatment.SOLID, headerTreatment(MatrixMaterialQuality.CHEAP))
    }
    @Test fun supportLossDisablesDialogEnhancementAtEveryTier() {
        MatrixMaterialQuality.entries.forEach { tier ->
            assertFalse(dialogBlurEnabled(tier, false))
            assertEquals(tier == MatrixMaterialQuality.FULL, dialogBlurEnabled(tier, true))
        }
    }
    @Test fun pressAndFocusLossRestoreLibraryRestingTarget() {
        assertEquals(1f, MatrixFocusMotion.target(true, true, 1.015f))
        assertEquals(1f, MatrixFocusMotion.target(false, true, 1.015f))
        assertEquals(1f, MatrixFocusMotion.target(false, false, 1.015f))
        assertEquals(1.015f, MatrixFocusMotion.target(true, false, 1.015f))
    }
    @Test fun themeFocusScaleIsPreservedIncludingDisabledScale() {
        for (scale in listOf(1f, 1.015f, 1.1f)) {
            assertEquals(scale, MatrixFocusMotion.target(true, false, scale))
            assertEquals(1f, MatrixFocusMotion.target(true, true, scale))
        }
    }
}
