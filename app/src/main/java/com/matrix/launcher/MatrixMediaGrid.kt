package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp

/** Shared results/list geometry and focus observation; neither surface owns a duplicate grid. */
@Composable
internal fun MatrixMediaGrid(cards: List<MediaCard>, windowWidth: androidx.compose.ui.unit.Dp, columns: Int, state: LazyGridState, repository: ComposeHomeRepository,
    restoreId: String?, initial: FocusRequester, focused: (Int, MediaCard) -> Unit, open: (MediaCard) -> Unit,
    modifier: Modifier = Modifier, footer: LazyGridScope.() -> Unit = {}) {
    val inspection = LocalMatrixUiInspection.current
    BoxWithConstraints(modifier) {
        val portraitOnly = cards.isNotEmpty() && cards.all { it.portrait }
        val posterWidth = MatrixLayout.preferredPortraitWidth(windowWidth, columns)
        val count = if (portraitOnly) MatrixLayout.fittingColumns(maxWidth, posterWidth, 10.dp) else columns.coerceIn(3, 6)
        val slotWidth = MatrixLayout.cardWidth(maxWidth, count, 10.dp)
        MatrixScrollViewport {
            LazyVerticalGrid(
                columns = GridCells.Fixed(count),
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        val isNav = event.key == Key.DirectionDown || event.key == Key.DirectionUp ||
                                    event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                        isNav && event.type == KeyEventType.KeyDown &&
                            ComposeRevealPolicy.deferRepeat(event.nativeKeyEvent.repeatCount, state.isScrollInProgress)
                    },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(MatrixLayout.gap),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }, contentType = { _, _ -> "media" }) { index, card ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        val width = if (portraitOnly) posterWidth.coerceAtMost(slotWidth) else MatrixLayout.mediaWidth(slotWidth, card.portrait, windowWidth, columns)
                        MatrixMediaCard(card, width, onFocus = {
                            // Grid card focus: do not mutate global Home backdrop palette during secondary grid navigation
                        },
                        modifier = (if (card.id == restoreId) Modifier.focusRequester(initial) else Modifier).onFocusChanged {
                            if (it.isFocused) {
                                focused(index, card); inspection?.focusedOption = index; inspection?.focusedState = "card"
                            } else if (inspection?.focusedOption == index) { inspection.focusedOption = -1; inspection.focusedState = "none" }
                        }) { open(card) }
                    }
                }
                footer()
            }
        }
    }
}
