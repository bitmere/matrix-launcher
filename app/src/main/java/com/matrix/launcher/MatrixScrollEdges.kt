@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.matrix.launcher

import androidx.core.graphics.withTranslation
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Fade only the short clipped strips into the exact parent backdrop; no full-screen layer. */
@Composable
internal fun Modifier.matrixScrollEdges(backward: () -> Boolean, forward: () -> Boolean): Modifier {
    val backdrop = LocalMatrixBackdrop.current ?: return this
    val location = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onGloballyPositioned { location[0] = it }.drawWithCache {
        val height = MatrixLayout.edgeFade.toPx().coerceAtMost(size.height / 2)
        fun mask(top: Boolean) = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(0f, if (top) 0f else size.height - height, 0f, if (top) height else size.height,
                if (top) Color.WHITE else Color.TRANSPARENT, if (top) Color.TRANSPARENT else Color.WHITE, Shader.TileMode.CLAMP)
        }
        val topMask = mask(true); val bottomMask = mask(false)
        onDrawWithContent {
            drawContent()
            val parent = backdrop.coordinates; val child = location[0]
            if (parent?.isAttached == true && child?.isAttached == true && height > 0) {
                val offset = child.positionInRoot() - parent.positionInRoot()
                drawIntoCanvas { wrapper ->
                    val canvas = wrapper.nativeCanvas
                    fun strip(top: Boolean) {
                        val start = if (top) 0f else size.height - height
                        val end = if (top) height else size.height
                        val layer = canvas.saveLayer(0f, start, size.width, end, null)
                        canvas.clipRect(0f, start, size.width, end)
                        canvas.withTranslation(-offset.x, -offset.y) { backdrop.draw(this) }
                        canvas.drawRect(0f, start, size.width, end, if (top) topMask else bottomMask)
                        canvas.restoreToCount(layer)
                    }
                    if (backward()) strip(true)
                    if (forward()) strip(false)
                }
            }
        }
    }
}

/** Vertical page focus stays clear of the fade; horizontal Home rows retain their own inset. */
@Composable
internal fun MatrixScrollViewport(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val inset = with(LocalDensity.current) { (MatrixLayout.edgeFade + MatrixLayout.focusInset).toPx() }
    val reveal = remember(inset) { object : BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
            ComposeRevealPolicy.distance(offset, size, containerSize, inset)
    } }
    CompositionLocalProvider(LocalBringIntoViewSpec provides reveal) {
        Box(modifier, propagateMinConstraints = true) { content() }
    }
}
