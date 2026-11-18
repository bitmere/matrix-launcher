package com.matrix.launcher

import kotlin.math.min
import kotlin.math.roundToInt

internal data class TextureSize(val width: Int, val height: Int)

/** At most a 720p texture, proportional to the actual surface and bounded by cache bytes. */
internal object BackdropQuality {
    const val cacheBytes = 16 * 1024 * 1024
    fun size(width: Int, height: Int): TextureSize {
        val scale = min(1f, min(1280f / width.coerceAtLeast(1), 720f / height.coerceAtLeast(1)))
        return TextureSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
    }
}
