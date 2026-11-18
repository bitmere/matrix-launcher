package com.matrix.launcher

/** Reveal clipped edges, but keep an already-visible card and its surrounding Home context in place. */
internal object ComposeRevealPolicy {
    /** Let a held D-pad advance again when its previous reveal has settled; never queue repeats. */
    fun deferRepeat(repeat: Int, scrolling: Boolean): Boolean = repeat > 0 && scrolling

    fun distance(offset: Float, extent: Float, viewport: Float, inset: Float = 0f): Float {
        if (viewport <= 0f || extent <= 0f) return 0f
        // An oversized target cannot fit at both edges. Align its start without oscillating.
        if (extent >= viewport) return offset
        val edge = inset.coerceIn(0f, (viewport - extent) / 2f)
        return when {
            offset < edge -> offset - edge
            offset + extent > viewport - edge -> offset + extent - viewport + edge
            else -> 0f
        }
    }
}
