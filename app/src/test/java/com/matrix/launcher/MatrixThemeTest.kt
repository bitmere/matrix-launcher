package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** JVM-only tests for the pure theme parser/validator (MatrixTheme). */
class MatrixThemeTest {

    private val validJson = """
        {"background":"#FF000000","panel":"#FF1C1E24","surface":"#FF242B34",
         "text":"#FFFFFFFF","textSecondary":"#FFD1D5DF","accent":"#FF4D9FFF",
         "radius":20,"gap":12,"focusScale":1.0,"tileSize":4}
    """.trimIndent()

    @Test fun validFileParses() {
        val state = MatrixTheme.parse(validJson)
        assertNull(state.ignoredReason)
        assertTrue(state.fromFile)
        assertEquals(0xFF000000.toInt(), state.background)
        assertEquals(0xFF1C1E24.toInt(), state.panel)
        assertEquals(0xFF4D9FFF.toInt(), state.accent)
        assertEquals(20f, state.radiusDp, 0.001f)
        assertEquals(12f, state.gapDp, 0.001f)
        assertEquals(1.0f, state.focusScale, 0.001f)
        assertEquals(4, state.tileSize)
    }

    @Test fun badColourFallsBackToDefaults() {
        val state = MatrixTheme.parse("""{"background":"not-a-colour","text":"#FFFFFFFF"}""")
        assertEquals(ThemeState.defaults().copy(ignoredReason = state.ignoredReason, fromFile = true), state)
        assertNotNull(state.ignoredReason)
        assertEquals(ThemeState.DEFAULT_BACKGROUND, state.background)
        assertEquals(ThemeState.DEFAULT_TEXT, state.text)
    }

    @Test fun outOfRangeRadiusIsRejected() {
        val state = MatrixTheme.parse("""{"radius":64}""")
        assertNotNull(state.ignoredReason)
        assertEquals(ThemeState.DEFAULT_RADIUS_DP, state.radiusDp, 0.001f)
        assertTrue(state.ignoredReason!!.contains("radius"))
    }

    @Test fun outOfRangeFocusScaleAndTileSizeAreRejected() {
        assertNotNull(MatrixTheme.parse("""{"focusScale":1.2}""").ignoredReason)
        assertNotNull(MatrixTheme.parse("""{"focusScale":0.9}""").ignoredReason)
        assertNotNull(MatrixTheme.parse("""{"tileSize":2}""").ignoredReason)
        assertNotNull(MatrixTheme.parse("""{"tileSize":7}""").ignoredReason)
        assertEquals(1.0f, MatrixTheme.parse("""{"focusScale":1.0}""").focusScale, 0.001f)
        assertEquals(6, MatrixTheme.parse("""{"tileSize":6}""").tileSize)
    }

    @Test fun lowContrastIsRejected() {
        // Dim grey text on black is below the 4.5 text/background minimum.
        val state = MatrixTheme.parse("""{"text":"#FF555555"}""")
        assertNotNull(state.ignoredReason)
        assertEquals(ThemeState.DEFAULT_TEXT, state.text)
        // Dim secondary text below the 3.0 minimum is rejected too.
        assertNotNull(MatrixTheme.parse("""{"textSecondary":"#FF222222"}""").ignoredReason)
    }

    @Test fun missingFileYieldsDefaults() {
        val emptyDir = Files.createTempDirectory("matrix-theme-test").toFile()
        try {
            val state = MatrixTheme.load(emptyDir)
            assertEquals(ThemeState.defaults(), state)
            assertNull(state.ignoredReason)
            assertFalse(state.fromFile)
        } finally {
            emptyDir.delete()
        }
    }

    @Test fun missingTokensFallBackIndividually() {
        val state = MatrixTheme.parse("""{"accent":"#FF00FF00"}""")
        assertNull(state.ignoredReason)
        assertEquals(0xFF00FF00.toInt(), state.accent)
        assertEquals(ThemeState.DEFAULT_BACKGROUND, state.background)
        assertEquals(ThemeState.DEFAULT_RADIUS_DP, state.radiusDp, 0.001f)
        assertEquals(ThemeState.DEFAULT_TILE_SIZE, state.tileSize)
    }

    @Test fun accentDefaultsToTextColour() {
        val state = MatrixTheme.parse("""{"text":"#FFEEEEEE"}""")
        assertNull(state.ignoredReason)
        assertEquals(0xFFEEEEEE.toInt(), state.accent)
    }

    @Test fun invalidJsonFallsBackWithReason() {
        val state = MatrixTheme.parse("{not json")
        assertNotNull(state.ignoredReason)
        assertEquals(ThemeState.DEFAULT_TEXT, state.text)
    }

    @Test fun loadReadsAFileFromDisk() {
        val dir = Files.createTempDirectory("matrix-theme-test").toFile()
        try {
            File(dir, THEME_FILE_NAME).writeText(validJson)
            val state = MatrixTheme.load(dir)
            assertNull(state.ignoredReason)
            assertEquals(4, state.tileSize)
            assertEquals(20f, state.radiusDp, 0.001f)
        } finally {
            File(dir, THEME_FILE_NAME).delete()
            dir.delete()
        }
    }

    @Test fun colourParsingAcceptsSixAndEightDigits() {
        assertEquals(0xFF112233.toInt(), MatrixTheme.parseColor("#112233"))
        assertEquals(0x80112233.toInt(), MatrixTheme.parseColor("#80112233"))
        assertNull(MatrixTheme.parseColor("#12345"))
        assertNull(MatrixTheme.parseColor("red"))
        assertNull(MatrixTheme.parseColor("#GGGGGG"))
    }

    @Test fun cycleWrapsAround() {
        assertEquals(2, MatrixTheme.cycle(listOf(1, 2, 3), 1))
        assertEquals(1, MatrixTheme.cycle(listOf(1, 2, 3), 3))
        assertEquals(1, MatrixTheme.cycle(listOf(1, 2, 3), 99))
    }

    @Test fun nextStateAdvancesOnlyOneToken() {
        val base = ThemeState.defaults()
        val next = MatrixTheme.nextState(base, "focusScale")
        assertEquals(1.03f, next.focusScale, 0.001f)
        assertEquals(base.background, next.background)
        assertEquals(base.tileSize, next.tileSize)
        assertEquals(base, MatrixTheme.nextState(base, "not-a-token"))
    }

    private fun withPreset(state: ThemeState, token: String, value: Any): ThemeState = when (token) {
        "background" -> state.copy(background = value as Int)
        "panel" -> state.copy(panel = value as Int)
        "surface" -> state.copy(surface = value as Int)
        "text" -> state.copy(text = value as Int)
        "textSecondary" -> state.copy(textSecondary = value as Int)
        "accent" -> state.copy(accent = value as Int)
        "radius" -> state.copy(radiusDp = value as Float)
        "gap" -> state.copy(gapDp = value as Float)
        "focusScale" -> state.copy(focusScale = value as Float)
        "tileSize" -> state.copy(tileSize = value as Int)
        else -> error("unknown token $token")
    }

    private fun tokenValue(state: ThemeState, token: String): Any = when (token) {
        "background" -> state.background
        "panel" -> state.panel
        "surface" -> state.surface
        "text" -> state.text
        "textSecondary" -> state.textSecondary
        "accent" -> state.accent
        "radius" -> state.radiusDp
        "gap" -> state.gapDp
        "focusScale" -> state.focusScale
        "tileSize" -> state.tileSize
        else -> error("unknown token $token")
    }

    @Test fun everyPresetRoundTripsThroughParse() {
        val cases: List<Pair<String, List<Any>>> = listOf(
            "background" to MatrixTheme.BACKGROUND_PRESETS,
            "panel" to MatrixTheme.PANEL_PRESETS,
            "surface" to MatrixTheme.SURFACE_PRESETS,
            "text" to MatrixTheme.TEXT_PRESETS,
            "textSecondary" to MatrixTheme.TEXT_SECONDARY_PRESETS,
            "accent" to MatrixTheme.ACCENT_PRESETS,
            "radius" to MatrixTheme.RADIUS_PRESETS,
            "gap" to MatrixTheme.GAP_PRESETS,
            "focusScale" to MatrixTheme.FOCUS_SCALE_PRESETS,
            "tileSize" to MatrixTheme.TILE_SIZE_PRESETS,
        )
        for ((token, values) in cases) {
            for (value in values) {
                val state = withPreset(ThemeState.defaults(), token, value)
                val parsed = MatrixTheme.parse(MatrixTheme.encode(state))
                assertNull("preset $token=$value rejected: ${parsed.ignoredReason}", parsed.ignoredReason)
                assertEquals(value, tokenValue(parsed, token))
            }
        }
    }

    @Test fun saveAndLoadRoundTrip() {
        val dir = Files.createTempDirectory("matrix-theme-test").toFile()
        try {
            val state = ThemeState.defaults().copy(focusScale = 1.05f, tileSize = 4, accent = 0xFF4DA3FF.toInt())
            MatrixTheme.save(dir, state)
            val loaded = MatrixTheme.load(dir)
            assertNull(loaded.ignoredReason)
            assertTrue(loaded.fromFile)
            assertEquals(1.05f, loaded.focusScale, 0.001f)
            assertEquals(4, loaded.tileSize)
            assertEquals(0xFF4DA3FF.toInt(), loaded.accent)
        } finally {
            File(dir, THEME_FILE_NAME).delete()
            dir.delete()
        }
    }
}
