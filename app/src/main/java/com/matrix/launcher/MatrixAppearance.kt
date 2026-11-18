package com.matrix.launcher

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

/** Process-local visual state only; theme roles and the rest of the UI do not subscribe to it. */
internal object MatrixAppearance {
    var palette by mutableStateOf<ArtworkPalette?>(null)
    fun focusApp(color: Int) { palette = ArtworkPalette.app(color) }
}

@Composable
internal fun MatrixPanel(wide: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val shape = if (wide) RectangleShape else RoundedCornerShape(topStart = MatrixLayout.panelCorner, bottomStart = MatrixLayout.panelCorner)
    val glassBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = .38f),
            Color.White.copy(alpha = .08f),
            Color.White.copy(alpha = .22f)
        )
    )
    MatrixBackdropSurface(
        modifier = Modifier
            .fillMaxHeight()
            .then(if (wide) Modifier.fillMaxWidth() else Modifier.width(MatrixLayout.panelWidth))
            .clip(shape)
            .border(width = 1.dp, brush = glassBorder, shape = shape),
        panel = true
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = MatrixLayout.panelInset, vertical = MatrixLayout.panelVerticalInset), content = content)
    }
}

/** Cancel obsolete artwork requests; briefly coalesce rapid D-pad repeats before sampling. */
@Composable
internal fun rememberMatrixArtworkFocus(repository: ComposeHomeRepository, active: Boolean = true): (String) -> Unit {
    val image = remember { MutableStateFlow("") }
    LaunchedEffect(repository, active) {
        if (active) image.collectLatest { uri ->
            if (uri.isNotEmpty()) { delay(80); repository.palette(uri)?.let { MatrixAppearance.palette = it } }
        }
    }
    return remember { { image.value = it } }
}
