package com.matrix.launcher

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class BackdropKey(val palette: ArtworkPalette, val base: Int, val width: Int, val height: Int, val panel: Boolean)
internal data class BackdropFrame(val previous: Bitmap?, val current: Bitmap)

/** Cached high-precision 720p gradients; no noise layer or per-frame shader generation. */
private object BackdropTextures {
    private val cache = object : android.util.LruCache<BackdropKey, Bitmap>(BackdropQuality.cacheBytes) {
        override fun sizeOf(key: BackdropKey, value: Bitmap) = value.allocationByteCount
    }
    @Synchronized fun get(palette: ArtworkPalette, base: Int, size: IntSize, panel: Boolean): Bitmap {
        val texture = BackdropQuality.size(size.width, size.height)
        val width = texture.width; val height = texture.height
        val key = BackdropKey(palette, base, width, height, panel)
        cache.get(key)?.let { return it }
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        if (panel) {
            // Liquid Glass Material 3 surface: pristine frosted depth, specular sheen, zero card-color staining.
            canvas.drawColor(0xE610131A.toInt(), android.graphics.PorterDuff.Mode.SRC)
            // Specular vertical gradient: subtle bright glass reflection at the top fading downward
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                intArrayOf(0x28ffffff.toInt(), 0x08ffffff.toInt(), 0x02000000, 0x22000000.toInt()),
                floatArrayOf(0f, 0.25f, 0.65f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Curved specular highlight at top edge
            paint.shader = RadialGradient(width * 0.45f, 0f, width * 0.65f,
                intArrayOf(0x1Effffff.toInt(), 0x04ffffff.toInt(), 0), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            cache.put(key, bitmap)
            return bitmap
        }
        canvas.drawColor(base)
        fun lobe(color: Int, alpha: Int, x: Float, y: Float, radius: Float) {
            paint.shader = RadialGradient(width * x, height * y, width * radius,
                intArrayOf((color and 0xffffff) or (alpha shl 24), color and 0xffffff), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        lobe(palette.primary, 84, .18f, .62f, .72f)
        lobe(palette.secondary, 65, .86f, .28f, .60f)
        lobe(palette.secondary, 30, .65f, .94f, .68f)
        cache.put(key, bitmap)
        return bitmap
    }
    fun snapshot(frame: BackdropFrame, fraction: Float): Bitmap {
        if (frame.previous == null || fraction >= .999f) return frame.current
        val bitmap = createBitmap(frame.current.width, frame.current.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(frame.previous, null, rect, paint)
        paint.alpha = (255 * fraction).toInt().coerceIn(0, 255)
        canvas.drawBitmap(frame.current, null, rect, paint)
        return bitmap
    }
}

/** One parent backdrop also supplies exact background pixels for short scroll-edge fades. */
@Stable
internal class MatrixBackdropState(val base: Int) {
    var frame by mutableStateOf<BackdropFrame?>(null)
    val fraction = Animatable(1f)
    var size by mutableStateOf(IntSize.Zero)
    var coordinates: LayoutCoordinates? = null
    // Shared photo resources; coordinates remain local to this window for edge fades.
    var glassSource: MatrixGlassSource? = null
    var panelLocal: Boolean = false
    var panelMaterial by mutableStateOf<MatrixGlass.Material?>(null)
    var panelSolid by mutableStateOf(false)
    private val panelPaint = Paint()
    private var panelStops: List<androidx.compose.ui.graphics.Color>? = null
    private var panelHeight = -1
    var panelOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var panelWindowSize by mutableStateOf(IntSize.Zero)
    private val windowInPanel = RectF()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    fun draw(canvas: Canvas) {
        if (panelLocal) { drawPanel(canvas); return }
        val value = frame
        if (value == null) { canvas.drawColor(base); drawWallpaper(canvas); return }
        rect.set(0f, 0f, size.width.toFloat(), size.height.toFloat())
        paint.alpha = 255
        value.previous?.let { canvas.drawBitmap(it, null, rect, paint) }
        paint.alpha = if (value.previous == null) 255 else (255 * fraction.value).toInt().coerceIn(0, 255)
        canvas.drawBitmap(value.current, null, rect, paint)
        drawWallpaper(canvas)
    }
    private fun drawPanel(canvas: Canvas) {
        val material = panelMaterial ?: return
        rect.set(0f, 0f, size.width.toFloat(), size.height.toFloat())
        if (panelSolid || glassSource?.blurred == null) canvas.drawColor(material.scrim.base)
        else {
            windowInPanel.set(-panelOffset.x, -panelOffset.y,
                panelWindowSize.width - panelOffset.x, panelWindowSize.height - panelOffset.y)
            glassSource?.drawFrost(canvas, windowInPanel)
        }
        if (panelStops != material.fillStops || panelHeight != size.height) {
            panelStops = material.fillStops; panelHeight = size.height
            panelPaint.shader = LinearGradient(0f, 0f, 0f, size.height.coerceAtLeast(1).toFloat(),
                material.fillStops.map { it.toArgb() }.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, panelPaint)
    }
    private fun drawWallpaper(canvas: Canvas) {
        if (panelLocal) return
        val source = glassSource ?: return
        val bitmap = source.decoded ?: return
        rect.set(0f, 0f, size.width.toFloat(), size.height.toFloat())
        paint.alpha = 255
        canvas.drawBitmap(bitmap, null, rect, paint)
        val dim = source.key?.dim ?: GlassDim()
        // Cached by source/configuration, not generated per frame.
        if (dimHeight != size.height || dimValue != dim) {
            dimHeight = size.height; dimValue = dim
            dimPaint.shader = LinearGradient(0f, 0f, 0f, size.height.toFloat(),
                intArrayOf((dim.top * 255).toInt() shl 24, (dim.middle * 255).toInt() shl 24,
                    (dim.bottom * 255).toInt() shl 24), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, dimPaint)
    }
    private val dimPaint = Paint()
    private var dimHeight = -1
    private var dimValue: GlassDim? = null
}
internal val LocalMatrixBackdrop = staticCompositionLocalOf<MatrixBackdropState?> { null }

@Composable
internal fun MatrixBackdropSurface(modifier: Modifier = Modifier, panel: Boolean = false, artworkBackdrop: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val cheap = LocalMatrixCapabilities.current.material == MatrixMaterialQuality.CHEAP
    val base = colorResource(if (panel) R.color.home_panel else R.color.home_background).toArgb().let { if (cheap) it or 0xff000000.toInt() else it }
    val fallback = colorResource(R.color.home_surface).toArgb()
    val palette = if (cheap || panel) null else if (artworkBackdrop) MatrixAppearance.palette ?: ArtworkPalette.app(fallback) else null
    val source = LocalMatrixGlassSource.current
    val localState = remember(base) { MatrixBackdropState(base) }
    // Menu/grid edge fades sample panel pixels, never activity coordinates across a Dialog window.
    val state = if (panel) localState else source?.backdrop ?: localState
    state.glassSource = source
    state.panelLocal = panel
    state.panelSolid = cheap
    val analysis = source?.analysis ?: WallpaperAnalysis.Empty
    val foreground = MaterialTheme.colorScheme.onSurface
    state.panelMaterial = remember(panel, analysis, foreground) {
        if (panel) MatrixGlass.material(MatrixGlass.Role.Panel, analysis, foreground) else null
    }
    val view = LocalView.current
    val transitions = LocalMatrixCapabilities.current.transitions
    LaunchedEffect(palette, state.size, transitions) {
        if (palette == null) { state.frame = null; state.fraction.snapTo(1f); return@LaunchedEffect }
        if (state.size == IntSize.Zero) return@LaunchedEffect
        delay(35)
        val old = state.frame
        val progress = state.fraction.value
        val (previous, target) = withContext(Dispatchers.Default) {
            val next = BackdropTextures.get(palette, base, state.size, panel)
            // Only snapshot the blended transition frame when mid-animation; skip the allocation at rest.
            val snap = if (old != null && progress < .999f) BackdropTextures.snapshot(old, progress) else old?.current
            snap to next
        }
        if (old?.current === target && progress >= .999f) return@LaunchedEffect
        state.fraction.snapTo(if (previous == null) 1f else 0f)
        state.frame = BackdropFrame(previous, target)
        if (previous != null && transitions) state.fraction.animateTo(1f, tween(320))
        state.frame = BackdropFrame(null, target)
    }
    Box(modifier.fillMaxSize().onSizeChanged { state.size = it; if (!panel) source?.size = it }.onGloballyPositioned {
            state.coordinates = it
            if (panel) {
                state.panelOffset = it.positionInWindow()
                state.panelWindowSize = IntSize(view.rootView.width, view.rootView.height)
            }
        }
        .drawBehind { drawIntoCanvas { state.draw(it.nativeCanvas) } }) {
        CompositionLocalProvider(LocalMatrixBackdrop provides state) { content() }
    }
}
