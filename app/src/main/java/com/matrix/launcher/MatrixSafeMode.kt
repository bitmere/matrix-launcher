package com.matrix.launcher

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Safe mode: a fallback Home with wallpaper off, AI discovery off, custom
 * rows off (only app channels + favorites), and the default layout.
 *
 * On when any of these holds: (a) the settings toggle ([MatrixPrefs.SAFE_MODE]),
 * (b) the launching intent carries the `safe_mode` boolean extra (per-launch,
 * not persisted — adb-triggerable), (c) the crash counter reaches 3 at startup.
 *
 * The counter increments on every onCreate before setContent (written with
 * commit(), so a crash before composition still counts) and resets after the
 * first successful composition. Reaching 3 sets `safe_mode` true and resets
 * the counter, so safe mode is entered once and the user can exit via the
 * toggle. Every gating point reads the same flag: [isOn].
 */
internal object MatrixSafeMode {
    const val CRASH_LIMIT = 3
    const val INTENT_EXTRA = "safe_mode"

    fun isOn(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(MatrixPrefs.SAFE_MODE, false)

    /** Column count in effect: the default when safe mode is on, else the saved value. */
    fun effectiveColumns(prefs: SharedPreferences): Int =
        if (isOn(prefs)) MatrixPrefs.LIST_COLUMNS_DEFAULT
        else prefs.getInt(MatrixPrefs.LIST_COLUMNS, MatrixPrefs.LIST_COLUMNS_DEFAULT)

    /** Pure flag/counter transition (no Android prefs): the JVM-testable seam. */
    data class Startup(val enabled: Boolean, val crashes: Int, val persistSafeMode: Boolean)

    fun transition(crashesBefore: Int, safeModePref: Boolean, intentSafeMode: Boolean): Startup {
        if (intentSafeMode) return Startup(enabled = true, crashes = crashesBefore + 1, persistSafeMode = false)
        if (safeModePref) return Startup(enabled = true, crashes = crashesBefore + 1, persistSafeMode = false)
        val crashes = crashesBefore + 1
        return if (crashes >= CRASH_LIMIT) {
            Startup(enabled = true, crashes = 0, persistSafeMode = true)
        } else {
            Startup(enabled = false, crashes = crashes, persistSafeMode = false)
        }
    }

    /**
     * Run in onCreate before setContent. Increments the crash counter with
     * commit(), applies the transition, and returns whether safe mode is on.
     */
    fun onStartup(prefs: SharedPreferences, intentSafeMode: Boolean): Boolean {
        val result = transition(
            crashesBefore = prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0),
            safeModePref = isOn(prefs),
            intentSafeMode = intentSafeMode
        )
        prefs.edit(commit = true) {
            putInt(MatrixPrefs.SAFE_MODE_CRASHES, result.crashes)
            if (result.persistSafeMode) putBoolean(MatrixPrefs.SAFE_MODE, true)
        }
        return result.enabled
    }

    /** Run after the first successful composition: a clean start resets the counter. */
    fun onFirstComposition(prefs: SharedPreferences) {
        if (prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0) != 0) {
            prefs.edit { putInt(MatrixPrefs.SAFE_MODE_CRASHES, 0) }
        }
    }

    fun setEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit { putBoolean(MatrixPrefs.SAFE_MODE, enabled) }
    }
}
