package com.matrix.launcher

internal data class ArtworkBounds(val width: Float, val height: Float)

/** The rim, clip and progress all use fitted source bounds, never letterbox space. */
internal object ArtworkFit {
    fun aspect(width: Int, height: Int, fallback: Float): Float =
        if (width > 0 && height > 0) width.toFloat() / height else fallback
    fun bounds(width: Float, height: Float, aspect: Float): ArtworkBounds {
        if (width <= 0 || height <= 0 || !aspect.isFinite() || aspect <= 0) return ArtworkBounds(0f, 0f)
        return if (width / height > aspect) ArtworkBounds(height * aspect, height) else ArtworkBounds(width, width / aspect)
    }
}
