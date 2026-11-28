package com.matrix.launcher

import android.graphics.RectF
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.tv.material3.MaterialTheme

/** Window-relative sampling works separately in Home and Dialog; never compare their coordinates. */
@Composable
internal fun Modifier.matrixGlassSurface(role: MatrixGlass.Role, frost: Boolean = true): Modifier {
    val source = LocalMatrixGlassSource.current
    val capabilities = LocalMatrixCapabilities.current
    val colors = MaterialTheme.colorScheme
    val analysis = source?.analysis ?: WallpaperAnalysis.Empty
    val material = remember(role, analysis, colors.onSurface) { MatrixGlass.material(role, analysis, colors.onSurface) }
    val view = LocalView.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val bounds = remember { RectF() }
    return onGloballyPositioned { offset = it.positionInWindow() }.drawBehind {
        val solid = capabilities.material == MatrixMaterialQuality.CHEAP || (frost && source?.blurred == null)
        if (solid) drawRect(Color(material.scrim.base))
        else if (frost) {
            bounds.set(-offset.x, -offset.y, view.rootView.width - offset.x, view.rootView.height - offset.y)
            drawIntoCanvas { source?.drawFrost(it.nativeCanvas, bounds) }
        }
        drawRect(material.fill)
        // A partial top edge, not an outer poster border.
        drawLine(material.edgeLight, Offset.Zero, Offset(size.width, 0f), MatrixGlass.edgeWidth.toPx())
    }
}
