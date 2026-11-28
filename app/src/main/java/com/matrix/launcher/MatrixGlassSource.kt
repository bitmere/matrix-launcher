package com.matrix.launcher

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Shared resource owner, not a window-coordinate owner. Panel fades retain their local coordinates. */
@Stable
internal class MatrixGlassSource(val backdrop: MatrixBackdropState) {
    val worker = Mutex()
    var size by mutableStateOf(IntSize.Zero)
    var key by mutableStateOf<GlassSourceKey?>(null)
    var analysis by mutableStateOf(WallpaperAnalysis.Empty)
    var decoded by mutableStateOf<Bitmap?>(null)
    var blurred by mutableStateOf<BackdropFrame?>(null)
    val fraction = Animatable(1f)
    var allocationFailed by mutableStateOf(false)
    var suspended by mutableStateOf(false)
    var refresh by mutableIntStateOf(0)
    private val frostPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    /** Caller clips its surface, then supplies the full window bounds in its local canvas.
     * No cross-window LayoutCoordinates and no independently stretched per-control crop. */
    fun drawFrost(canvas: Canvas, windowInLocal: RectF) {
        val frame = blurred ?: return
        frostPaint.alpha = 255
        frame.previous?.let { canvas.drawBitmap(it, null, windowInLocal, frostPaint) }
        frostPaint.alpha = if (frame.previous == null) 255 else (fraction.value * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(frame.current, null, windowInLocal, frostPaint)
    }
    fun clear() {
        decoded = null; blurred = null; key = null; analysis = WallpaperAnalysis.Empty
        backdrop.frame = null
    }
    fun publish(key: GlassSourceKey, decoded: Bitmap, blur: Bitmap, analysis: WallpaperAnalysis) {
        val previous = blurred?.current
        check(blur.allocationByteCount + (previous?.allocationByteCount ?: 0) <= GlassBudget.outputBytes)
        this.key = key; this.decoded = decoded; this.analysis = analysis
        blurred = BackdropFrame(previous, blur)
    }
}
internal val LocalMatrixGlassSource = staticCompositionLocalOf<MatrixGlassSource?> { null }

@Composable
internal fun rememberMatrixReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(!android.animation.ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = !android.animation.ValueAnimator.areAnimatorsEnabled() }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

@Composable
internal fun rememberMatrixGlassSource(repository: ComposeHomeRepository, uri: String?, generation: Long, reduced: Boolean): MatrixGlassSource {
    val context = LocalContext.current.applicationContext
    val base = colorResource(R.color.home_background).toArgb()
    val source = remember(base) { MatrixGlassSource(MatrixBackdropState(base)) }
    val density = LocalDensity.current.density
    val capabilities = LocalMatrixCapabilities.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(source, lifecycle) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            private fun trim() {
                source.suspended = !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                source.clear()
                source.refresh++
            }
            override fun onLowMemory() = trim()
            override fun onTrimMemory(level: Int) = trim()
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) source.suspended = false
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { source.suspended = true; source.clear() }
        }
        context.registerComponentCallbacks(callbacks)
        lifecycle.addObserver(observer)
        onDispose { context.unregisterComponentCallbacks(callbacks); lifecycle.removeObserver(observer); source.clear() }
    }
    LaunchedEffect(uri, generation, source.size, density, base, capabilities.wallpaper, source.suspended, source.refresh, reduced, capabilities.transitions) {
        if (!capabilities.wallpaper || uri == null || source.suspended || source.allocationFailed) {
            source.clear(); return@LaunchedEffect
        }
        if (source.size == IntSize.Zero) return@LaunchedEffect
        // Discard a previous transition before starting another worker: at most two retained outputs.
        source.blurred = source.blurred?.let { BackdropFrame(null, it.current) }
        val (width, height) = GlassBudget.size(source.size.width, source.size.height)
        val key = GlassSourceKey(uri, generation, width, height, source.size.width, source.size.height,
            density, GlassCrop(), 18f, base, GlassDim())
        if (key == source.key) {
            source.fraction.snapTo(1f)
            return@LaunchedEffect
        }
        try {
            val result = source.worker.withLock { withContext(Dispatchers.IO) {
                val analysis = WallpaperAnalysis.from(repository.palette(uri, generation) ?: ArtworkPalette.app(base))
                val display = key.width to key.height
                // The unblurred source remains independently bounded to 1920x1080-class.
                val scale = minOf(1f, 1920f / key.displayWidth, 1080f / key.displayHeight)
                val dw = max(1, (key.displayWidth * scale).toInt())
                val dh = max(1, (key.displayHeight * scale).toInt())
                val request = ImageRequest.Builder(context).data(uri).size(dw, dh).allowHardware(false)
                    .scale(coil.size.Scale.FIT).precision(coil.size.Precision.EXACT)
                    .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
                val loaded = coil.Coil.imageLoader(context).execute(request) as? SuccessResult
                    ?: error("Wallpaper unavailable")
                coroutineContext.ensureActive()
                val drawable = loaded.drawable
                val decodedBitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (decodedBitmap != null && decodedBitmap.allocationByteCount > GlassBudget.decodedBytes)
                    throw OutOfMemoryError("Wallpaper decode exceeded the bounded source budget")
                val bitmap = createBitmap(dw, dh)
                check(bitmap.allocationByteCount <= GlassBudget.decodedBytes)
                val canvas = Canvas(bitmap)
                val ratio = max(dw.toFloat() / drawable.intrinsicWidth.coerceAtLeast(1), dh.toFloat() / drawable.intrinsicHeight.coerceAtLeast(1))
                val sw = drawable.intrinsicWidth.coerceAtLeast(1) * ratio
                val sh = drawable.intrinsicHeight.coerceAtLeast(1) * ratio
                drawable.setBounds(((dw - sw) * key.crop.x).roundToInt(), ((dh - sh) * key.crop.y).roundToInt(),
                    ((dw - sw) * key.crop.x + sw).roundToInt(), ((dh - sh) * key.crop.y + sh).roundToInt())
                drawable.draw(canvas)
                val blur = withContext(Dispatchers.Default) { wallpaperBlur(bitmap, display.first, display.second, key.sourceRadiusPx) }
                Triple(bitmap, blur, analysis)
            } }
            coroutineContext.ensureActive()
            if (source.suspended) return@LaunchedEffect
            source.fraction.snapTo(if (source.blurred == null || reduced || !capabilities.transitions) 1f else 0f)
            source.publish(key, result.first, result.second, result.third)
            if (source.fraction.value < 1f) source.fraction.animateTo(1f, tween(320))
            source.blurred = source.blurred?.let { BackdropFrame(null, it.current) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) { source.clear(); source.allocationFailed = true }
        catch (_: Exception) { source.clear() }
    }
    return source
}

/** Separable, edge-clamped box blur, one worker per owner. No frame or focus-path work.
 * Edge extension supplies radius padding without allocating an expanded bitmap. */
private suspend fun wallpaperBlur(source: Bitmap, width: Int, height: Int, radius: Float): Bitmap {
    require(width * height <= GlassBudget.maxPixels)
    val bitmap = createBitmap(width, height)
    Canvas(bitmap).drawBitmap(source, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    val pixels = IntArray(width * height)
    val scratch = IntArray(pixels.size)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val r = radius.roundToInt().coerceIn(1, 24)
    suspend fun pass(input: IntArray, output: IntArray, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val divisor = r * 2 + 1
        for (line in 0 until lines) {
            coroutineContext.ensureActive()
            fun index(position: Int) = if (horizontal) line * width + position.coerceIn(0, length - 1) else position.coerceIn(0, length - 1) * width + line
            var red = 0; var green = 0; var blue = 0
            fun add(pixel: Int, sign: Int) { red += (pixel ushr 16 and 255) * sign; green += (pixel ushr 8 and 255) * sign; blue += (pixel and 255) * sign }
            for (offset in -r..r) add(input[index(offset)], 1)
            for (position in 0 until length) {
                output[index(position)] = 0xff000000.toInt() or ((red / divisor) shl 16) or ((green / divisor) shl 8) or (blue / divisor)
                add(input[index(position - r)], -1); add(input[index(position + r + 1)], 1)
            }
        }
    }
    pass(pixels, scratch, true); pass(scratch, pixels, false)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
