package com.matrix.launcher

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
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
    val analysis = LocalMatrixGlassSource.current?.analysis ?: WallpaperAnalysis.Empty
    val foreground = androidx.tv.material3.MaterialTheme.colorScheme.onSurface
    val glassBorder = remember(analysis, foreground) {
        MatrixGlass.material(MatrixGlass.Role.Panel, analysis, foreground).edgeLight
    }
    MatrixBackdropSurface(
        modifier = Modifier
            .fillMaxHeight()
            .then(if (wide) Modifier.fillMaxWidth() else Modifier.width(MatrixLayout.panelWidth))
            .clip(shape)
            .border(width = MatrixGlass.edgeWidth, brush = glassBorder, shape = shape),
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
