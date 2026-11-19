package com.matrix.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixSearchOrbTest {
    @Test fun rotationStartsAtZeroAndCompletesOneTurnPerPeriod() {
        assertEquals(0f, orbRotationDegrees(0L), 0.001f)
        assertEquals(90f, orbRotationDegrees(1500L), 0.001f)
        assertEquals(180f, orbRotationDegrees(3000L), 0.001f)
        assertEquals(0f, orbRotationDegrees(6000L), 0.001f)
        assertEquals(90f, orbRotationDegrees(7500L), 0.001f)
    }

    @Test fun rotationPeriodIsSlowEnoughToReadAsCalmButNotFrozen() {
        assertTrue(ORB_ROTATION_PERIOD_MS >= 2000L)
        assertTrue(ORB_ROTATION_PERIOD_MS <= 10000L)
    }

    @Test fun swirlPaletteIsTheSiriIdentityInSweepOrder() {
        val colors = orbSwirlColors()
        // Five hues plus the wrap repeat, so the sweep has no seam.
        assertEquals(6, colors.size)
        assertEquals(SIRI_BLUE, colors[0])
        assertEquals(SIRI_CYAN, colors[1])
        assertEquals(SIRI_PURPLE, colors[2])
        assertEquals(SIRI_PINK, colors[3])
        assertEquals(SIRI_ORANGE, colors[4])
        assertEquals(colors[0], colors[5])
        // The five hues are distinct colours.
        assertEquals(5, colors.dropLast(1).toSet().size)
    }

    @Test fun swirlColoursAreSiriIdentityNotThemeTokens() {
        assertEquals(Color(0xFF3385FF), SIRI_BLUE)
        assertEquals(Color(0xFF2ED9F2), SIRI_CYAN)
        assertEquals(Color(0xFF9E4DFF), SIRI_PURPLE)
        assertEquals(Color(0xFFFF458C), SIRI_PINK)
        assertEquals(Color(0xFFFF9E33), SIRI_ORANGE)
        // The dark base lets the swirl read as luminous.
        assertEquals(listOf(ORB_BASE_CENTER, ORB_BASE_EDGE), orbBaseColors())
        assertTrue(ORB_BASE_CENTER != ORB_BASE_EDGE)
    }

    @Test fun orbIsBigEnoughToReadAtTenFeetAndNeverAHero() {
        assertTrue(ORB_SIZE_DP.value >= 48f)
        assertTrue(ORB_SIZE_DP.value <= 64f)
    }

    @Test fun glowAndCoreStayInsideTheOrbBounds() {
        // Containment: nothing is drawn beyond the orb radius.
        assertTrue(ORB_GLOW_RADIUS_FRACTION <= 1.0f)
        // The bright core is well inside the orb.
        assertTrue(ORB_CORE_RADIUS_FRACTION > 0f && ORB_CORE_RADIUS_FRACTION < 1.0f)
        assertEquals(55f, orbCoreRadius(100f), 0.001f)
        // The core sits up-left of centre, inside the orb.
        val core = orbCoreCenter(Offset(100f, 100f), 100f)
        assertEquals(75f, core.x, 0.001f)
        assertEquals(72f, core.y, 0.001f)
        assertTrue(core.x > 0f && core.y > 0f)
        // The rim and alphas stay faint: soft, not a halo.
        assertTrue(ORB_RIM_DP.value <= 2f)
        assertTrue(ORB_CORE_ALPHA in 0.3f..0.7f)
        assertTrue(ORB_RIM_ALPHA <= 0.35f)
        assertTrue(ORB_EDGE_GLOW_ALPHA <= 0.15f)
    }
}
