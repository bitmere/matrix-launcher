package com.matrix.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matrix.launcher.core.HomeCardSizing

/** Shared measured spacing. Reserve only gaps between slots, never an extra trailing gap. */
internal object MatrixLayout {
    /** Observed tokens allow startup to render defaults while disk loading runs on IO. */
    var theme by mutableStateOf(ThemeState.defaults())
    val panelWidth = 440.dp
    val panelInset = 30.dp
    val panelVerticalInset = 28.dp
    val edgeFade = 24.dp
    /** Focus zoom token; default 1.015. Compare against 1.0 with the bench (tv-bench skill). */
    val cardFocusScale: Float get() = theme.focusScale
    const val focusFadeMillis = 160
    const val artworkFadeMillis = 120
    val focusInset = 8.dp
    val focusOutlineOffset = 1.dp
    val focusOutlineWidth = 2.dp
    val focusHaloSpread = 8.dp
    // TV Material clips card content: keep the entire outline inside its image slot.
    val artworkInset = focusOutlineOffset + focusOutlineWidth / 2 + focusHaloSpread
    val channelInset = 52.dp
    val dockInset = 12.dp
    /** Gap token; default 16dp. */
    val gap: Dp get() = theme.gapDp.dp
    /** Radius token; default 24dp. */
    val panelCorner: Dp get() = theme.radiusDp.dp
    /** Card corners keep the current 24:14 proportion to the radius token; default 14dp. */
    val cardCorner: Dp get() = (theme.radiusDp * 14f / 24f).dp
    val playbackTrackHeight = 3.dp
    fun cardWidth(width: Dp, columns: Int, horizontalInset: Dp): Dp =
        ((width - horizontalInset * 2 - gap * (columns.coerceAtLeast(1) - 1)) / columns.coerceAtLeast(1)).coerceAtLeast(1.dp)
    /** Portrait artwork retains 2:3 without inheriting a landscape card's oversized width. */
    fun mediaWidth(landscapeWidth: Dp, portrait: Boolean): Dp =
        if (portrait) (landscapeWidth * .625f).coerceAtMost(144.dp) else landscapeWidth

    fun fittingColumns(width: Dp, desiredCard: Dp, horizontalInset: Dp): Int =
        ((width - horizontalInset * 2 + gap) / (desiredCard + gap)).toInt().coerceIn(1, 10)

    @androidx.compose.runtime.Immutable
    data class RowFit(val columns: Int, val cardWidth: Int, val gap: Int, val viewport: Int)

    /** Work in measured pixels so rounding cannot expose a sliver of the next slot. */
    fun rowFit(viewport: Int, desiredCard: Int, inset: Int, gap: Int, fixedColumns: Int? = null): RowFit {
        val available = (viewport - inset * 2).coerceAtLeast(1)
        val columns = (fixedColumns ?: ((available + gap) / (desiredCard + gap).coerceAtLeast(1)))
            .coerceIn(1, 10)
        val width = if (fixedColumns == null) desiredCard.coerceIn(1, available)
            else HomeCardSizing.slotWidth(available, columns, gap)
        // Poster size is a user preference; absorb spare width in spacing, not the artwork.
        val fittedGap = if (fixedColumns == null && columns > 1)
            ((available - width * columns) / (columns - 1)).coerceAtLeast(gap) else gap
        val fitted = width * columns + fittedGap * (columns - 1) + inset * 2
        return RowFit(columns, width, fittedGap, fitted.coerceAtMost(viewport))
    }
}
