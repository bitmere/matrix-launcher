package com.matrix.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Library scale targets must all be 1: this is the sole changing scale, not a multiplier. */
@Composable
internal fun Modifier.matrixFocusScale(interaction: MutableInteractionSource): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        MatrixFocusMotion.target(focused, pressed, MatrixLayout.cardFocusScale),
        if (LocalMatrixCapabilities.current.transitions) spring(
            stiffness = MatrixFocusMotion.stiffness, dampingRatio = MatrixFocusMotion.damping,
            visibilityThreshold = MatrixFocusMotion.threshold) else snap(),
        visibilityThreshold = MatrixFocusMotion.threshold, label = "Matrix focus scale")
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
