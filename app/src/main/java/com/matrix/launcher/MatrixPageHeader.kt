package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

internal val LocalMatrixPagePath = staticCompositionLocalOf { "Home" }

/** A single transparent header: physical Back remains primary; no footer or divider. */
@Composable
internal fun MatrixPageHeader(title: String, back: () -> Unit, backFocus: FocusRequester? = null,
    backLabel: String = "Back", actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = MatrixLayout.gap),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MatrixIconButton(R.drawable.ic_matrix_back, backLabel, back, compact = true,
            modifier = if (backFocus == null) Modifier else Modifier.focusRequester(backFocus))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(LocalMatrixPagePath.current, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

/** Loading and error pages also have a reachable initial focus target. */
@Composable
internal fun MatrixStatusPage(title: String, message: String, loading: Boolean, back: () -> Unit) {
    val focus = remember { FocusRequester() }
    Column { MatrixPageHeader(title, back, focus); MatrixLoadState(message, loading) }
    LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
}
