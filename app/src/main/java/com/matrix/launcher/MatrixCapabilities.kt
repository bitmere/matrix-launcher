package com.matrix.launcher

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

internal enum class MatrixMaterialQuality { FULL, STATIC, CHEAP }
internal enum class MatrixMotionPolicy { NORMAL, REDUCED }

/** Material, motion and cross-window support are independent runtime decisions. */
internal data class MatrixCapabilities(
    val material: MatrixMaterialQuality,
    val motion: MatrixMotionPolicy = MatrixMotionPolicy.NORMAL,
    val crossWindowBlur: Boolean = false,
) {
    val liveBlur get() = material == MatrixMaterialQuality.FULL
    val wallpaper get() = material != MatrixMaterialQuality.CHEAP
    val animations get() = material == MatrixMaterialQuality.FULL && motion == MatrixMotionPolicy.NORMAL
    val transitions get() = material != MatrixMaterialQuality.CHEAP && motion == MatrixMotionPolicy.NORMAL
    companion object {
        val FULL = MatrixCapabilities(MatrixMaterialQuality.FULL)
        val STATIC = MatrixCapabilities(MatrixMaterialQuality.STATIC)
        val CHEAP = MatrixCapabilities(MatrixMaterialQuality.CHEAP)
        /** measuredGpu must come from device profiling, never SDK inference. Defaults closed. */
        fun decide(safeMode: Boolean, lowRam: Boolean, sdkInt: Int, measuredGpu: Boolean = false,
                   allocationFailed: Boolean = false, lowEffects: Boolean = false,
                   reducedMotion: Boolean = false, crossWindowSupported: Boolean = false): MatrixCapabilities {
            val tier = when {
                safeMode || lowRam || allocationFailed || lowEffects -> MatrixMaterialQuality.CHEAP
                sdkInt >= 31 && measuredGpu -> MatrixMaterialQuality.FULL
                else -> MatrixMaterialQuality.STATIC
            }
            return MatrixCapabilities(tier, if (reducedMotion) MatrixMotionPolicy.REDUCED else MatrixMotionPolicy.NORMAL,
                sdkInt >= 31 && crossWindowSupported)
        }
    }
}
internal val LocalMatrixCapabilities: ProvidableCompositionLocal<MatrixCapabilities> =
    compositionLocalOf { MatrixCapabilities.STATIC }
