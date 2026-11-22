package com.matrix.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixControlBorderTest {
    private val onSurface = Color(0xFFEDEDED)
    private val error = Color(0xFFB3261E)
    private val errorContainer = Color(0xFFF9DEDC)

    @Test fun ordinaryUnfocusedBorderIsNone() {
        for (role in listOf(MatrixControlRole.Option, MatrixControlRole.Action,
            MatrixControlRole.Commit, MatrixControlRole.Secondary)) {
            val (border, _) = matrixControlBorders(role, onSurface, error, errorContainer)
            assertEquals("role $role", Border.None, border)
        }
    }

    @Test fun ordinaryFocusedBorderIsSolidOnSurface() {
        val (_, focused) = matrixControlBorders(MatrixControlRole.Option, onSurface, error, errorContainer)
        assertEquals(1.5.dp, focused.border.width)
        assertEquals(SolidColor(onSurface), focused.border.brush)
    }

    @Test fun destructiveUnfocusedBorderStays() {
        val (border, _) = matrixControlBorders(MatrixControlRole.Destructive, onSurface, error, errorContainer)
        assertEquals(1.dp, border.border.width)
        assertEquals(SolidColor(errorContainer.copy(alpha = .55f)), border.border.brush)
    }

    @Test fun destructiveFocusedBorderIsSolidError() {
        val (_, focused) = matrixControlBorders(MatrixControlRole.Destructive, onSurface, error, errorContainer)
        assertEquals(1.5.dp, focused.border.width)
        assertEquals(SolidColor(error), focused.border.brush)
    }
}
