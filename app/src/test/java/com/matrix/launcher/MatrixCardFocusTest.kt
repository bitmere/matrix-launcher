package com.matrix.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Border
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixCardFocusTest {
    @Test fun focusedBorderIsNoneSoOnlyArtworkGlowRimFramesFocus() {
        assertEquals(Border.None, focusedCardBorder())
    }

    @Test fun titleColorAndWeightFollowFocus() {
        val onSurface = Color(ThemeState.DEFAULT_TEXT)
        val onSurfaceVariant = Color(ThemeState.DEFAULT_TEXT_SECONDARY)

        assertEquals(onSurface, cardTitleColor(true, onSurface, onSurfaceVariant))
        assertEquals(FontWeight.Medium, cardTitleWeight(true))
        assertEquals(onSurfaceVariant, cardTitleColor(false, onSurface, onSurfaceVariant))
        assertEquals(FontWeight.Normal, cardTitleWeight(false))
    }
}
