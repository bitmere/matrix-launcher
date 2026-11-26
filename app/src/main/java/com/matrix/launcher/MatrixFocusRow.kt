@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

/** Outer alignment is outside scrolling, so ordinary D-pad reveal cannot consume it. */
@Composable
internal fun MatrixFocusRow(state: LazyListState, horizontalInset: Dp, preferredWidth: Dp, rowKey: String,
    availableWidth: Dp,
    columns: Int? = null, verticalInset: Dp = MatrixLayout.focusInset, fallback: FocusRequester = FocusRequester.Default,
    isLastRow: Boolean = false,
    content: LazyListScope.(Dp) -> Unit) {
    val density = LocalDensity.current
    val inset = with(density) { MatrixLayout.focusInset.roundToPx() }
    val gap = with(density) { MatrixLayout.gap.roundToPx() }
    val inspection = LocalMatrixUiInspection.current
    val outerInset = (horizontalInset - MatrixLayout.focusInset).coerceAtLeast(0.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = outerInset)) {
        // Home already measured this width; do not subcompose every channel to measure it again.
        val viewport = with(density) { (availableWidth - outerInset * 2).coerceAtLeast(1.dp).roundToPx() }
        val fit = MatrixLayout.rowFit(viewport, with(density) { preferredWidth.roundToPx() }, inset, gap, columns)
        val width = with(density) { fit.cardWidth.toDp() }
        val reveal = remember(inset) { object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
                ComposeRevealPolicy.distance(offset, size, containerSize, inset.toFloat())
        } }
        DisposableEffect(inspection, rowKey, state, fit) {
            inspection?.rows?.set(rowKey, MatrixRowGeometry(state, fit.columns, fit.cardWidth))
            onDispose { inspection?.rows?.remove(rowKey) }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides reveal) {
            LazyRow(state = state,
                modifier = Modifier.width(with(density) { fit.viewport.toDp() }).focusRestorer(fallback).focusProperties {
                    onEnter = {
                        if (fallback != FocusRequester.Default) fallback.requestFocus()
                    }
                    onExit = {
                        if (requestedFocusDirection == FocusDirection.Left || requestedFocusDirection == FocusDirection.Right) cancelFocusChange()
                        if (isLastRow && requestedFocusDirection == FocusDirection.Down) cancelFocusChange()
                    }
                }.focusGroup(),
                contentPadding = PaddingValues(horizontal = MatrixLayout.focusInset, vertical = verticalInset),
                horizontalArrangement = Arrangement.spacedBy(with(density) { fit.gap.toDp() })) { content(width) }
        }
    }
}
