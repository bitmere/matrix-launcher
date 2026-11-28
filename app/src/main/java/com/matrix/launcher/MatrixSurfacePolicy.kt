package com.matrix.launcher

internal enum class HeaderTreatment { LIVE, FROST, SOLID }
internal fun headerTreatment(tier: MatrixMaterialQuality) = when (tier) {
    MatrixMaterialQuality.FULL -> HeaderTreatment.LIVE
    MatrixMaterialQuality.STATIC -> HeaderTreatment.FROST
    MatrixMaterialQuality.CHEAP -> HeaderTreatment.SOLID
}

internal fun dialogBlurEnabled(tier: MatrixMaterialQuality, supported: Boolean) =
    tier == MatrixMaterialQuality.FULL && supported

internal object MatrixFocusMotion {
    const val stiffness = 600f
    const val damping = 1f
    const val threshold = .00075f
    fun target(focused: Boolean, pressed: Boolean, focusedScale: Float) =
        if (pressed || !focused) 1f else focusedScale
}

