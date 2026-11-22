package com.matrix.launcher

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

/** JVM-only tests for the safe-mode flag/counter transition (MatrixSafeMode). */
class MatrixSafeModeTest {

    @Test fun firstCrashesDoNotEnterSafeMode() {
        assertEquals(MatrixSafeMode.Startup(enabled = false, crashes = 1, persistSafeMode = false),
            MatrixSafeMode.transition(crashesBefore = 0, safeModePref = false, intentSafeMode = false))
        assertEquals(MatrixSafeMode.Startup(enabled = false, crashes = 2, persistSafeMode = false),
            MatrixSafeMode.transition(crashesBefore = 1, safeModePref = false, intentSafeMode = false))
    }

    @Test fun thirdCrashEntersSafeModeOnceAndResetsCounter() {
        val result = MatrixSafeMode.transition(crashesBefore = 2, safeModePref = false, intentSafeMode = false)
        assertTrue(result.enabled)
        assertEquals(0, result.crashes)
        assertTrue(result.persistSafeMode)
    }

    @Test fun intentExtraEntersSafeModeWithoutPersisting() {
        val result = MatrixSafeMode.transition(crashesBefore = 0, safeModePref = false, intentSafeMode = true)
        assertTrue(result.enabled)
        assertFalse(result.persistSafeMode)
        assertEquals(1, result.crashes)
    }

    @Test fun toggleOnStaysOn() {
        val result = MatrixSafeMode.transition(crashesBefore = 0, safeModePref = true, intentSafeMode = false)
        assertTrue(result.enabled)
        assertFalse(result.persistSafeMode)
    }

    @Test fun onStartupAndFirstCompositionRoundTripWithMapBackedPrefs() {
        val prefs = mapBackedPrefs()
        // Two crashing starts: still off, counter at 2.
        assertFalse(MatrixSafeMode.onStartup(prefs, intentSafeMode = false))
        assertFalse(MatrixSafeMode.onStartup(prefs, intentSafeMode = false))
        assertEquals(2, prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0))
        // Third crashing start: safe mode on, counter reset, flag persisted.
        assertTrue(MatrixSafeMode.onStartup(prefs, intentSafeMode = false))
        assertEquals(0, prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0))
        assertTrue(prefs.getBoolean(MatrixPrefs.SAFE_MODE, false))
        // A clean start stays in safe mode (toggle exits it) and resets the counter.
        assertTrue(MatrixSafeMode.onStartup(prefs, intentSafeMode = false))
        MatrixSafeMode.onFirstComposition(prefs)
        assertEquals(0, prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0))
        // Exiting via the toggle turns it off.
        MatrixSafeMode.setEnabled(prefs, false)
        assertFalse(MatrixSafeMode.isOn(prefs))
        assertFalse(MatrixSafeMode.onStartup(prefs, intentSafeMode = false))
        // Intent extra enters safe mode for one launch without persisting.
        assertTrue(MatrixSafeMode.onStartup(prefs, intentSafeMode = true))
        assertFalse(prefs.getBoolean(MatrixPrefs.SAFE_MODE, false))
    }

    @Test fun intentOnlySafeModeUsesDefaultColumnsAndNormalModeKeepsSavedSize() {
        val prefs = mapBackedPrefs()
        for (columns in 3..6) {
            prefs.edit().putInt(MatrixPrefs.LIST_COLUMNS, columns).apply()
            assertFalse(MatrixSafeMode.isOn(prefs))
            assertEquals(MatrixPrefs.LIST_COLUMNS_DEFAULT, MatrixSafeMode.effectiveColumns(prefs, true))
            assertEquals(columns, MatrixSafeMode.effectiveColumns(prefs, false))
        }
        prefs.edit().putBoolean(MatrixPrefs.SAFE_MODE, true).apply()
        assertEquals(6, MatrixSafeMode.effectiveColumns(prefs, false))
    }

    @Test fun effectiveColumnsDefaultInSafeMode() {
        val prefs = mapBackedPrefs()
        prefs.edit().putInt(MatrixPrefs.LIST_COLUMNS, 3).apply()
        assertEquals(3, MatrixSafeMode.effectiveColumns(prefs, MatrixSafeMode.isOn(prefs)))
        prefs.edit().putBoolean(MatrixPrefs.SAFE_MODE, true).apply()
        assertEquals(MatrixPrefs.LIST_COLUMNS_DEFAULT, MatrixSafeMode.effectiveColumns(prefs, MatrixSafeMode.isOn(prefs)))
    }
}
