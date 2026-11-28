package com.matrix.launcher

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Pure policy: sampled colours describe tint, never a bound on local wallpaper brightness. */
internal data class WallpaperAnalysis(val tint: Int, val luminance: Double) {
    companion object {
        val Empty = from(ArtworkPalette(0xff20242a.toInt(), 0xff20242a.toInt()))
        fun from(palette: ArtworkPalette) = WallpaperAnalysis(palette.primary,
            (GlassContrast.luminance(palette.primary) + GlassContrast.luminance(palette.secondary)) / 2)
    }
}

internal object GlassContrast {
    const val text = 4.5
    const val largeTextAndBoundary = 3.0
    fun luminance(rgb: Int): Double {
        fun linear(shift: Int): Double {
            val c = (rgb ushr shift and 255) / 255.0
            return if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
        }
        return .2126 * linear(16) + .7152 * linear(8) + .0722 * linear(0)
    }
    fun ratio(a: Int, b: Int): Double {
        val x = luminance(a); val y = luminance(b)
        return (max(x, y) + .05) / (min(x, y) + .05)
    }
    fun over(foreground: Int, alpha: Double, background: Int): Int {
        val a = alpha.coerceIn(0.0, 1.0)
        fun channel(shift: Int) = (((foreground ushr shift and 255) * a +
            (background ushr shift and 255) * (1 - a)).roundToInt() shl shift)
        return 0xff000000.toInt() or channel(16) or channel(8) or channel(0)
    }
    /** Includes tint and the strongest highlight. Unknown imagery is bounded by BOTH black and white.
     * Returns an opaque contrasting fill if an imported foreground cannot meet the target with tint.
     * Consumers must use the returned tint/highlight alphas, including the opaque fallback. */
    fun scrim(foreground: Int, tint: Int, tintAlpha: Double, highlightAlpha: Double,
              minimum: Double, target: Double = text): GlassScrim {
        val base = if (luminance(foreground) > .179) 0xff000000.toInt() else -1
        for (step in (minimum * 1000).roundToInt()..1000) {
            val alpha = step / 1000.0
            fun composite(bg: Int, highlight: Int, edge: Double) = over(highlight, edge, over(tint, tintAlpha, over(base, alpha, bg)))
            val extremes = listOf(0xff000000.toInt(), -1)
            if (extremes.all { bg -> extremes.all { highlight ->
                    listOf(0.0, highlightAlpha).all { edge -> ratio(foreground, composite(bg, highlight, edge)) >= target }
                } })
                return GlassScrim(base, alpha, tintAlpha, highlightAlpha)
        }
        return GlassScrim(base, 1.0, 0.0, 0.0)
    }
}
internal data class GlassScrim(val base: Int, val alpha: Double, val tintAlpha: Double, val highlightAlpha: Double)
internal data class GlassDim(val top: Float = .65f, val middle: Float = .35f, val bottom: Float = .85f)
internal data class GlassCrop(val x: Float = .5f, val y: Float = .5f)
internal data class GlassSourceKey(
    val identity: String, val generation: Long, val width: Int, val height: Int,
    val displayWidth: Int, val displayHeight: Int, val density: Float,
    val crop: GlassCrop, val radiusDp: Float, val theme: Int, val dim: GlassDim,
) {
    val sourceRadiusPx get() = radiusDp.coerceIn(0f, 24f) * density * width / displayWidth.coerceAtLeast(1)
}
internal object GlassBudget {
    const val decodedBytes = 1920 * 1080 * 4
    const val decodeScratchBytes = decodedBytes * 2
    const val outputBytes = 4 * 1024 * 1024
    const val maxPixels = 854 * 480
    // Two pixel arrays plus one working bitmap, separate from current/previous outputs.
    const val scratchBytes = maxPixels * 12
    fun size(width: Int, height: Int): Pair<Int, Int> {
        val scale = min(1.0, min(854.0 / width.coerceAtLeast(1), 480.0 / height.coerceAtLeast(1)))
        return max(1, (width * scale).toInt()) to max(1, (height * scale).toInt())
    }
}
