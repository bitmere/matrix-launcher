package com.matrix.launcher

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Which effects Home may use. One decision point for old and new TVs:
 * safe mode or a low-RAM device gets the cheap variant (no glow sweep, no
 * wallpaper), everything else gets the full one.
 *
 * Pure and JVM-testable: the activity computes it once from MatrixSafeMode
 * and ActivityManager, then provides it to composition. Feature gates (SIMKL,
 * AI discovery) stay with MatrixSafeMode; this object covers effects only.
 */
internal data class MatrixCapabilities(val animations: Boolean, val wallpaper: Boolean) {
    companion object {
        /** Full effects: animations on, wallpaper on. */
        val FULL = MatrixCapabilities(animations = true, wallpaper = true)

        /** Cheap variant: no glow sweep, plain background. */
        val CHEAP = MatrixCapabilities(animations = false, wallpaper = false)

        fun decide(safeMode: Boolean, lowRam: Boolean): MatrixCapabilities =
            if (safeMode || lowRam) CHEAP else FULL
    }
}

/** Provided by the Home activity; defaults to full effects (previews, tests). */
internal val LocalMatrixCapabilities: ProvidableCompositionLocal<MatrixCapabilities> =
    compositionLocalOf { MatrixCapabilities.FULL }
