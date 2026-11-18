package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixPageNavigationTest {
    @Test fun focusReturnsToIdentityAfterReorderingAndFallsBackAfterRemoval() {
        assertEquals(2, matrixRestoreIndex(listOf("a", "b", "c"), "c"))
        assertEquals(0, matrixRestoreIndex(listOf("c", "a", "b"), "c"))
        assertEquals(0, matrixRestoreIndex(listOf("a", "b"), "removed"))
        assertEquals(0, matrixRestoreIndex(emptyList(), "removed"))
    }
    @Test fun backFirstResetsListThenLeavesIt() {
        org.junit.Assert.assertTrue(matrixBackResetsList("list", 5, true))
        org.junit.Assert.assertTrue(matrixBackResetsList("list", 1, false))
        org.junit.Assert.assertFalse(matrixBackResetsList("list", 1, true))
        org.junit.Assert.assertFalse(matrixBackResetsList("settings", 5, false))
    }

    @Test fun directEntryHasNoInventedSettingsParent() {
        assertEquals("Home", matrixPagePath(emptyList()))
        assertEquals("Home › Favorites", matrixPagePath(listOf("favorites")))
    }
    @Test fun nestedEntryKeepsActualHierarchyAndReadableSetupNames() {
        assertEquals("Home › Settings › Appearance › Wallpapers", matrixPagePath(listOf("settings", "appearance", "wallpapers")))
        assertEquals("Home › Settings › SIMKL › SIMKL setup", matrixPagePath(listOf("settings", "simkl", "simkl-client")))
    }
    @Test fun loadingOrErrorCannotRestoreHiddenResults() {
        org.junit.Assert.assertTrue(matrixResultsVisible(5, false, ""))
        org.junit.Assert.assertFalse(matrixResultsVisible(5, true, ""))
        org.junit.Assert.assertFalse(matrixResultsVisible(5, false, "Unavailable"))
        org.junit.Assert.assertFalse(matrixResultsVisible(0, false, ""))
    }
    @Test fun wallpaperBrowserHasRecognizableParent() {
        assertEquals("Home › Settings › Wallpapers › Import pictures",
            matrixPagePath(listOf("settings", "wallpapers", "wallpaper-device")))
        assertEquals("Home › Wallpapers › Saved wallpapers",
            matrixPagePath(listOf("wallpapers", "wallpaper-gallery")))
    }
}
