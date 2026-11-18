package com.matrix.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

/** A bounded viewport, rather than an oversized item in a focus-driven menu. */
@Composable
internal fun MatrixReleaseNotesScreen(title: String, notes: String, back: () -> Unit) {
    BackHandler(onBack = back)
    val scroll = rememberScrollState()
    val initial = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    Column {
        MatrixPageHeader(title, back)
        MatrixScrollViewport(Modifier.weight(1f)) {
            Text(notes, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        val direction = when (event.key) {
                            Key.DirectionDown -> 1
                            Key.DirectionUp -> -1
                            else -> return@onPreviewKeyEvent false
                        }
                        // At the top, Up can reach the header. Consume Down at
                        // the end so focus cannot escape into the underlying page.
                        if (direction < 0 && !scroll.canScrollBackward) false
                        else {
                            if (event.type == KeyEventType.KeyDown) scope.launch {
                                scroll.scrollTo((scroll.value + direction * (scroll.viewportSize / 4).coerceAtLeast(1))
                                    .coerceIn(0, scroll.maxValue))
                            }
                            true
                        }
                    }
                    .matrixScrollEdges({ scroll.canScrollBackward }, { scroll.canScrollForward })
                    .verticalScroll(scroll).focusRequester(initial).focusable()
                    .padding(vertical = 16.dp))
        }
    }
    LaunchedEffect(Unit) { withFrameNanos { }; initial.requestFocus() }
}
