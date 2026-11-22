package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixOrderToggleTest {
    @Test fun toggleHidesShownRow() {
        val (next, shown) = toggleOrderRowHidden(emptySet(), "row-a")
        assertEquals(setOf("row-a"), next)
        assertFalse(shown)
    }

    @Test fun toggleShowsHiddenRow() {
        val (next, shown) = toggleOrderRowHidden(setOf("row-a"), "row-a")
        assertEquals(emptySet<String>(), next)
        assertTrue(shown)
    }

    @Test fun toggleLeavesOtherRowsAlone() {
        val (next, shown) = toggleOrderRowHidden(setOf("row-a"), "row-b")
        assertEquals(setOf("row-a", "row-b"), next)
        assertFalse(shown)
    }
}
