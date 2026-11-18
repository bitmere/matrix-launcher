package com.matrix.launcher

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLaunchTest {
    @Test fun targetSelectionWorksWithoutNuvioAndNeverOverridesAnExplicitMissingChoice() {
        val nuvio = MatrixMediaApp.NUVIO; val stremio = MatrixMediaApp.STREMIO
        assertEquals(stremio, mediaTargetSelection("auto", listOf(stremio)))
        assertEquals(nuvio, mediaTargetSelection("auto", listOf(stremio, nuvio)))
        assertEquals(stremio, mediaTargetSelection("STREMIO", listOf(nuvio, stremio)))
        assertNull(mediaTargetSelection("NUVIO", listOf(stremio)))
        assertNull(mediaTargetSelection("unknown", listOf(nuvio, stremio)))
        assertNull(mediaTargetSelection("auto", emptyList()))
    }
    @Test fun eachAdapterUsesItsOwnWarmDeliveryContract() {
        val fresh = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        assertEquals(fresh, mediaDetailFlags(MatrixMediaApp.STREMIO))
        assertEquals(fresh or Intent.FLAG_ACTIVITY_SINGLE_TOP, mediaDetailFlags(MatrixMediaApp.NUVIO))
    }
    @Test fun nuvioContentDeliversEverySelectionWithoutClearingTheTaskOrForwardingGrants() {
        val expected = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(expected, mediaLaunchFlags(0, true))
        assertEquals(expected, mediaLaunchFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION, true))
    }
    @Test fun warmChannelLaunchKeepsPublisherRoutingWithoutForwardingGrantsOrTaskClearing() {
        val routing = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val unsafe = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or routing, mediaLaunchFlags(routing or unsafe))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, mediaLaunchFlags(0))
    }

    @Test fun missingMediaAppThrowsFriendlyInstallMessage() = kotlinx.coroutines.runBlocking {
        val selected = mediaTargetSelection("auto", emptyList())
        assertNull(selected)

        val prefs = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java)
        ) { _, method, _ ->
            if (method.name == "getString") "auto" else null
        } as android.content.SharedPreferences

        val router = MatrixMediaRouter(null, prefs, availableOverride = { emptyList() }) { _, _, _ -> "" }
        val card = MediaCard(
            id = "cinemeta:tt0133093",
            title = "The Matrix",
            image = "",
            link = "matrix-title://meta/movie/tt0133093",
            source = com.matrix.launcher.core.MediaLinks.SOURCE,
            portrait = true,
            position = 0L,
            duration = 0L
        )

        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { router.intent(card) }
        }
        assertEquals("To stream movies and series, install Stremio or Nuvio on your TV.", exception.message)
    }
}
