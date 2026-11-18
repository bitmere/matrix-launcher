package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeRevealPolicyTest {
    @Test fun repeatBackpressureOnlyAppliesToHeldKeysDuringScroll() {
        assertTrue(ComposeRevealPolicy.deferRepeat(3, true))
        assertEquals(false, ComposeRevealPolicy.deferRepeat(0, true))
        assertEquals(false, ComposeRevealPolicy.deferRepeat(3, false))
    }
    @org.junit.Test fun focusReservesRoomForTheOutlineAtBothEdges() {
        org.junit.Assert.assertEquals(-6f, ComposeRevealPolicy.distance(2f, 100f, 500f, 8f), 0f)
        org.junit.Assert.assertEquals(6f, ComposeRevealPolicy.distance(398f, 100f, 500f, 8f), 0f)
        org.junit.Assert.assertEquals(0f, ComposeRevealPolicy.distance(20f, 100f, 500f, 8f), 0f)
    }
    @org.junit.Test fun oversizedTargetsStayAlignedWithoutAlternatingEdges() {
        org.junit.Assert.assertEquals(0f, ComposeRevealPolicy.distance(0f, 600f, 500f, 8f), 0f)
        org.junit.Assert.assertEquals(-20f, ComposeRevealPolicy.distance(-20f, 600f, 500f, 8f), 0f)
    }
    @Test fun visibleFavoriteKeepsHeaderAndClippedEdgesMoveOnlyAsFarAsNeeded() {
        assertEquals(0f, ComposeRevealPolicy.distance(300f, 160f, 540f), 0f)
        assertEquals(120f, ComposeRevealPolicy.distance(500f, 160f, 540f), 0f)
        assertEquals(-24f, ComposeRevealPolicy.distance(-24f, 160f, 540f), 0f)
        assertEquals(0f, ComposeRevealPolicy.distance(0f, 160f, 0f), 0f)
    }
    @Test fun verticalPageRevealKeepsFocusedContentOutsideTheSoftFade() {
        val fade = MatrixLayout.edgeFade.value
        val safe = fade + MatrixLayout.focusInset.value
        val movement = ComposeRevealPolicy.distance(398f, 100f, 500f, safe)
        assertTrue(398f - movement + 100f <= 500f - fade - MatrixLayout.focusInset.value)
    }

}
