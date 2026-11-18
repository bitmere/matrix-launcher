package com.matrix.launcher

/** Two sampled colors. App banners use a restrained nearby companion, without extra decoding. */
internal data class ArtworkPalette(val primary: Int, val secondary: Int) {
    companion object {
        fun app(color: Int): ArtworkPalette {
            val r = color ushr 16 and 255; val g = color ushr 8 and 255; val b = color and 255
            return ArtworkPalette(color, (0xff000000.toInt() or (((r * 3 + g) / 4) shl 16)
                or (((g * 3 + b) / 4) shl 8) or ((b * 3 + r) / 4)))
        }
    }
}
