package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class GlassPolicyTest {
    @Test fun wcagReferenceEndpoints() {
        assertEquals(0.0, GlassContrast.luminance(0xff000000.toInt()), .000001)
        assertEquals(1.0, GlassContrast.luminance(-1), .000001)
        assertEquals(21.0, GlassContrast.ratio(-1, 0xff000000.toInt()), .000001)
        assertEquals(1.0, GlassContrast.ratio(-1, -1), .000001)
    }
    @Test fun wcagPrimaryCoefficients() {
        assertEquals(.2126, GlassContrast.luminance(0xffff0000.toInt()), .000001)
        assertEquals(.7152, GlassContrast.luminance(0xff00ff00.toInt()), .000001)
        assertEquals(.0722, GlassContrast.luminance(0xff0000ff.toInt()), .000001)
    }
    @Test fun tintUsesWallpaperPrimaryAndMeanLinearLuminance() {
        val result = WallpaperAnalysis.from(ArtworkPalette(-1, 0xff000000.toInt()))
        assertEquals(-1, result.tint); assertEquals(.5, result.luminance, .000001)
    }
    @Test fun thirtyFivePercentDimIsInsufficient() {
        assertTrue(GlassContrast.ratio(-1, GlassContrast.over(0xff000000.toInt(), .35, -1)) < 4.5)
    }
    @Test fun finalCompositeMeetsTextAndBoundaryTargets() {
        for (fg in listOf(-1, 0xffbbbbbb.toInt(), 0xff777777.toInt(), 0xff000000.toInt()))
            for (tint in listOf(-1, 0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0xff000000.toInt()))
                for (target in listOf(4.5, 3.0)) for (minimum in listOf(.60, .72, .85)) {
                    val rule = GlassContrast.scrim(fg, tint, .08, .15, minimum, target)
                    for (bg in listOf(-1, 0xff808080.toInt(), 0xff000000.toInt())) {
                        val final = GlassContrast.over(-1, rule.highlightAlpha,
                            GlassContrast.over(tint, rule.tintAlpha, GlassContrast.over(rule.base, rule.alpha, bg)))
                        assertTrue("foreground=$fg tint=$tint target=$target", GlassContrast.ratio(fg, final) >= target)
                    }
                }
    }
    @Test fun opaqueFallbackRemovesTintAndHighlightWhenNecessary() {
        val rule = GlassContrast.scrim(0xff777777.toInt(), -1, .08, .3, .60)
        assertEquals(1.0, rule.alpha, 0.0); assertEquals(0.0, rule.tintAlpha, 0.0)
        assertEquals(0.0, rule.highlightAlpha, 0.0)
    }
    private val key = GlassSourceKey("wallpaper", 1, 854, 480, 1920, 1080, 2f, GlassCrop(), 18f, 0, GlassDim())
    @Test fun sameUriNewContentGenerationInvalidates() { assertNotEquals(key, key.copy(generation = 2)) }
    @Test fun everyRenderingInputInvalidates() {
        val changes = listOf(key.copy(identity = "other"), key.copy(width = 800), key.copy(height = 400),
            key.copy(displayWidth = 3840), key.copy(displayHeight = 2160), key.copy(density = 1f),
            key.copy(crop = GlassCrop(.2f)), key.copy(radiusDp = 24f), key.copy(theme = 1),
            key.copy(dim = GlassDim(middle = .5f)))
        changes.forEach { assertNotEquals(key, it) }
        assertEquals(key, key.copy())
    }
    @Test fun radiusScalesFromDisplayToSourcePixels() {
        assertEquals(18f * 2f * 854 / 1920, key.sourceRadiusPx, .0001f)
        assertEquals(24f * 2f * 854 / 1920, key.copy(radiusDp = 100f).sourceRadiusPx, .0001f)
    }
    @Test fun outputAndScratchBudgetsHoldForAllAspectRatios() {
        for ((w, h) in listOf(3840 to 2160, 1920 to 1080, 1080 to 1920, 8000 to 100, 1 to 1, 0 to 0)) {
            val (width, height) = GlassBudget.size(w, h)
            assertTrue(width * height <= GlassBudget.maxPixels)
            assertTrue(width * height * 8 <= GlassBudget.outputBytes)
            assertTrue(width * height * 12 <= GlassBudget.scratchBytes)
        }
    }
}
