package com.matrix.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MatrixCardFocusTest {
    @Test fun focusedBorderUsesThemeColorAndInsetArtworkShape() {
        val onSurface = Color(ThemeState.DEFAULT_TEXT)
        val border = focusedCardBorder(onSurface)

        assertNotEquals(Border.None, border)
        assertEquals(2.dp, border.border.width)
        assertEquals(1.dp, border.inset)
        assertEquals(SolidColor(onSurface), border.border.brush)
        assertEquals(MatrixCardDefaults.artworkShape, border.shape)
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
