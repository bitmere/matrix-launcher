package com.matrix.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.tv.material3.IconButton
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.LocalContentColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/** Semantic hierarchy is shared across settings, forms, headers and editors. */
internal enum class MatrixControlRole { Option, Action, Commit, Secondary, Destructive }

/** One visual treatment for every settings choice, command, navigation and form control. */
internal object MatrixControlStyle {
    val shape = RoundedCornerShape(50)
    val minHeight = 52.dp
    val padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    val iconSize = 24.dp
    val inlineIconSize = 20.dp
    val toolbarButtonSize = 54.dp
    val headerButtonSize = 48.dp
    val toolbarSlotWidth = 60.dp
    val fieldHeight = 56.dp
    // Clear material over the existing dimmed backdrop: light defines the edge, not a dark fill.
    fun fill(colors: ColorScheme) = colors.onSurface.copy(alpha = .12f)
    fun stroke(colors: ColorScheme) = BorderStroke(.75.dp, Brush.linearGradient(listOf(
        colors.onSurface.copy(alpha = .60f), colors.onSurface.copy(alpha = .12f),
        colors.onSurface.copy(alpha = .36f))))
    @Composable
    fun glass(colors: ColorScheme): Brush = remember(colors.onSurface) {
        Brush.verticalGradient(listOf(colors.onSurface.copy(alpha = .15f),
            colors.onSurface.copy(alpha = .035f), colors.onSurface.copy(alpha = .08f)))
    }
}

/** Sweep for 12 s (three 4 s revolutions) when focus lands, then hold still: the
 * angle at 1080 equals the angle at 0 for the glow shader (sin/cos periodic),
 * so the held frame looks identical to the sweep start and Home draws 0 frames
 * at rest. The glow stays visible long enough to be seen. Read the angle
 * only during drawing. */
@Composable
internal fun rememberMatrixFocusRotation(focused: Boolean, enabled: Boolean = true): State<Float> {
    val rotation = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(focused, lifecycle, enabled) {
        rotation.snapTo(0f)
        if (enabled && focused) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            rotation.snapTo(0f)
            rotation.animateTo(1080f, tween(12000, easing = LinearEasing))
        }
    }
    return rotation.asState()
}

/** Cached artwork-coloured rim and soft outward plumes; no blur, bitmap or per-frame objects. */
internal class MatrixGlassBorder(private val bounds: android.graphics.RectF, private val radius: Float,
                                private val stroke: Float, palette: ArtworkPalette, private val spread: Float) {
    companion object {
        // One small bounded draw on Android 13+, instead of eight overlapping stroke draws.
        private const val GLOW = """
            uniform float2 center;
            uniform float2 halfSize;
            uniform float radius;
            uniform float spread;
            uniform float angle;
            layout(color) uniform half4 primary;
            layout(color) uniform half4 secondary;
            half4 main(float2 p) {
                float2 q = abs(p - center) - (halfSize - float2(radius));
                float d = length(max(q, float2(0))) + min(max(q.x, q.y), 0) - radius;
                if (d < 0 || d > spread) return half4(0);
                float a = atan(p.y - center.y, p.x - center.x) - angle;
                float peak = 0.5 + 0.5 * sin(a * 3);
                float reach = spread * (0.42 + 0.16 * peak);
                float fade = exp(-d * d / (reach * reach));
                float strength = fade * (0.18 + 0.62 * peak * peak);
                return mix(primary, secondary, 0.5 + 0.5 * sin(a)) * strength;
            }
        """
    }
    private val matrix = android.graphics.Matrix()
    private fun luminous(color: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[2] = hsv[2].coerceAtLeast(.85f)
        return android.graphics.Color.HSVToColor(hsv)
    }
    private val primary = luminous(palette.primary)
    private val secondary = luminous(palette.secondary)
    private val light = androidx.core.graphics.ColorUtils.blendARGB(primary, android.graphics.Color.WHITE, .30f)
    private val shader = android.graphics.SweepGradient(bounds.centerX(), bounds.centerY(),
        intArrayOf(primary, light, secondary, primary), null)
    private val plume = android.graphics.SweepGradient(bounds.centerX(), bounds.centerY(),
        intArrayOf(primary and 0x00ffffff, primary, primary and 0x00ffffff, secondary,
            secondary and 0x00ffffff, primary, primary and 0x00ffffff),
        floatArrayOf(0f, .12f, .32f, .50f, .70f, .87f, 1f))
    private val glowBounds = android.graphics.RectF(bounds).apply { inset(-spread, -spread) }
    private val glowShader = if (android.os.Build.VERSION.SDK_INT >= 33) runCatching {
        android.graphics.RuntimeShader(GLOW).apply {
            setFloatUniform("center", bounds.centerX(), bounds.centerY())
            setFloatUniform("halfSize", bounds.width() / 2, bounds.height() / 2)
            setFloatUniform("radius", radius)
            setFloatUniform("spread", spread)
            setColorUniform("primary", primary)
            setColorUniform("secondary", secondary)
        }
    }.getOrElse {
        android.util.Log.w("MatrixGlow", "Using compatible stroke glow", it)
        null
    } else null
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
    }
    fun drawGlow(canvas: android.graphics.Canvas, degrees: Float, alpha: Float = 1f) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && glowShader != null) {
            glowShader.setFloatUniform("angle", (degrees * Math.PI / 180).toFloat())
            paint.shader = glowShader
            paint.style = android.graphics.Paint.Style.FILL
            paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
            canvas.drawRect(glowBounds, paint)
            return
        }
        matrix.setRotate(degrees, bounds.centerX(), bounds.centerY())
        plume.setLocalMatrix(matrix)
        paint.shader = plume
        paint.style = android.graphics.Paint.Style.STROKE
        // Eight overlapping low-alpha bands soften the edge; angular peaks form moving plumes.
        val breath = .88f + .12f * kotlin.math.sin(degrees * Math.PI / 60).toFloat()
        for (band in 8 downTo 1) {
            val distance = band / 8f
            paint.strokeWidth = stroke + spread * distance * breath * 2
            paint.alpha = (255 * alpha * .22f * (1f - distance * .85f)).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(bounds, radius, radius, paint)
        }
    }
    fun draw(canvas: android.graphics.Canvas, degrees: Float, alpha: Float = 1f) {
        matrix.setRotate(degrees, bounds.centerX(), bounds.centerY())
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }
}

/** Caches one [MatrixGlassBorder] instance per card so glow/rim leaves skip redundant shader allocation. */
@Stable
internal class MatrixGlassBorderHolder {
    private var border: MatrixGlassBorder? = null
    private var width = -1f
    private var height = -1f
    private var offset = -1f
    private var radius = -1f
    private var stroke = -1f
    private var spread = -1f
    private var palette: ArtworkPalette? = null

    fun resolve(
        width: Float,
        height: Float,
        offset: Float,
        radius: Float,
        stroke: Float,
        spread: Float,
        palette: ArtworkPalette
    ): MatrixGlassBorder? {
        if (width <= 0f || height <= 0f) return null
        if (border == null ||
            this.width != width ||
            this.height != height ||
            this.offset != offset ||
            this.radius != radius ||
            this.stroke != stroke ||
            this.spread != spread ||
            this.palette != palette
        ) {
            this.width = width
            this.height = height
            this.offset = offset
            this.radius = radius
            this.stroke = stroke
            this.spread = spread
            this.palette = palette
            border = MatrixGlassBorder(
                android.graphics.RectF(0f - offset, 0f - offset, width + offset, height + offset),
                radius,
                stroke,
                palette,
                spread
            )
        }
        return border
    }
}

@Composable
internal fun MatrixButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null, role: MatrixControlRole = MatrixControlRole.Action,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, onLongClick = onLongClick, modifier = modifier.heightIn(min = MatrixControlStyle.minHeight),
        scale = ButtonDefaults.scale(focusedScale = MatrixLayout.cardFocusScale),
        shape = ButtonDefaults.shape(MatrixControlStyle.shape),
        colors = matrixControlColors(role), border = matrixControlBorder(role),
        contentPadding = MatrixControlStyle.padding) {
        androidx.tv.material3.ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Spacer(Modifier.height(MatrixControlStyle.minHeight - MatrixControlStyle.padding.calculateTopPadding() - MatrixControlStyle.padding.calculateBottomPadding()))
            if (role == MatrixControlRole.Commit) {
                Icon(painterResource(R.drawable.ic_matrix_check), null, Modifier.size(MatrixControlStyle.inlineIconSize))
                Spacer(Modifier.width(8.dp))
            }
            content()
        }
    }
}

/** Correct icon-only geometry; shares the text controls' visual tokens, never their row padding. */
@Composable
internal fun MatrixIconButton(icon: Int, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, role: MatrixControlRole = MatrixControlRole.Action, compact: Boolean = false, contentAlpha: Float = 1f) {
    val inspection = LocalMatrixUiInspection.current
    val geometry = remember(label) { MatrixIconGeometry() }
    DisposableEffect(inspection, label) {
        inspection?.icons?.put(label, geometry)
        onDispose { if (inspection?.icons?.get(label) === geometry) inspection.icons.remove(label) }
    }
    IconButton(onClick = onClick,
        modifier = modifier.size(if (compact) MatrixControlStyle.headerButtonSize else MatrixControlStyle.toolbarButtonSize)
            .onGloballyPositioned { geometry.button = it.boundsInRoot() },
        shape = ButtonDefaults.shape(MatrixControlStyle.shape),
        scale = ButtonDefaults.scale(focusedScale = MatrixLayout.cardFocusScale),
        colors = matrixControlColors(role), border = matrixControlBorder(role)) {
        Icon(painterResource(icon), label,
            Modifier.size(MatrixControlStyle.iconSize).onGloballyPositioned { geometry.icon = it.boundsInRoot() },
            tint = LocalContentColor.current.copy(alpha = contentAlpha))
    }
}

@Composable
private fun matrixControlColors(role: MatrixControlRole = MatrixControlRole.Action): ButtonColors {
    val colors = MaterialTheme.colorScheme
    val destructive = role == MatrixControlRole.Destructive
    return ButtonDefaults.colors(
        containerColor = if (destructive) colors.errorContainer else MatrixControlStyle.fill(colors),
        contentColor = if (destructive) colors.onErrorContainer else colors.onSurface,
        focusedContainerColor = if (destructive) colors.error else colors.primary,
        focusedContentColor = if (destructive) colors.onError else colors.onPrimary)
}

@Composable
private fun matrixControlBorder(role: MatrixControlRole = MatrixControlRole.Action): ButtonBorder {
    val colors = MaterialTheme.colorScheme
    val (border, focusedBorder) = matrixControlBorders(role, colors.onSurface, colors.error, colors.onErrorContainer)
    return ButtonDefaults.border(border = border, focusedBorder = focusedBorder)
}

/**
 * Pure border selection for [matrixControlBorder], so the choice is JVM-testable
 * without composition. The ordinary unfocused border is [Border.None]: rows are
 * separated by spacing and fill, not by a pale stroke. The destructive
 * unfocused border stays — it marks a destructive action, it is not white.
 */
internal fun matrixControlBorders(role: MatrixControlRole, onSurface: Color, error: Color,
                                  errorContainer: Color): Pair<Border, Border> {
    val border = if (role == MatrixControlRole.Destructive)
        Border(BorderStroke(1.dp, errorContainer.copy(alpha = .55f))) else Border.None
    val focusedBorder = Border(if (role == MatrixControlRole.Destructive)
        BorderStroke(1.5.dp, error) else BorderStroke(1.5.dp, onSurface))
    return border to focusedBorder
}

/** Titles and supporting text share a leading edge, rhythm and focus-aware foreground. */
@Composable
internal fun MatrixRowText(title: String, detail: String, modifier: Modifier = Modifier) {
    if (detail.isBlank()) {
        Text(title, modifier = modifier, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = .78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
