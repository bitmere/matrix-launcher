package com.matrix.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeLiveApplyTest {
    private val defaults = ThemeState.defaults()

    @Test fun identicalStatesDoNotNeedRefresh() {
        assertFalse(MatrixTheme.needsDataRefresh(defaults, defaults.copy()))
    }

    @Test fun tileSizeChangesNeedRefresh() {
        assertTrue(MatrixTheme.needsDataRefresh(defaults, defaults.copy(tileSize = 4)))
        assertTrue(MatrixTheme.needsDataRefresh(defaults.copy(tileSize = 3), defaults.copy(tileSize = 6)))
    }

    @Test fun colourChangeDoesNotNeedRefresh() {
        assertFalse(MatrixTheme.needsDataRefresh(defaults, defaults.copy(accent = ThemeState.DEFAULT_TEXT_SECONDARY)))
    }

    @Test fun layoutTokenChangesDoNotNeedRefresh() {
        assertFalse(MatrixTheme.needsDataRefresh(defaults, defaults.copy(radiusDp = 16f)))
        assertFalse(MatrixTheme.needsDataRefresh(defaults, defaults.copy(gapDp = 12f)))
        assertFalse(MatrixTheme.needsDataRefresh(defaults, defaults.copy(focusScale = 1f)))
    }

    @Test fun resettingThreeColumnsToDefaultsNeedsRefresh() {
        assertTrue(MatrixTheme.needsDataRefresh(defaults.copy(tileSize = 3), defaults))
    }

    @Test fun tileSizeAndColourChangeNeedsRefresh() {
        val next = defaults.copy(tileSize = 4, accent = ThemeState.DEFAULT_TEXT_SECONDARY)
        assertTrue(MatrixTheme.needsDataRefresh(defaults, next))
    }
}
