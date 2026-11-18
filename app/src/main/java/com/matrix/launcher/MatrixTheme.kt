package com.matrix.launcher

import org.json.JSONObject
import java.io.File

/** Level-0 theme tokens, loaded from `theme.json` in the app's files dir. */
internal data class ThemeState(
    val background: Int,
    val panel: Int,
    val surface: Int,
    val text: Int,
    val textSecondary: Int,
    /** Defaults to the text colour; maps to the colour scheme's `primary`. */
    val accent: Int,
    val radiusDp: Float,
    val gapDp: Float,
    val focusScale: Float,
    /** Maps to LIST_COLUMNS (range 3–6). */
    val tileSize: Int,
    /** Non-null when a theme file was present but ignored; shown on the Theme page. */
    val ignoredReason: String? = null,
    val fromFile: Boolean = false
) {
    companion object {
        // Defaults are the current values: colours from home_design.xml,
        // radius/gap/focusScale from MatrixLayout, tileSize from LIST_COLUMNS.
        const val DEFAULT_BACKGROUND = 0xFF000000.toInt()
        const val DEFAULT_PANEL = 0xFF1C1E24.toInt()
        const val DEFAULT_SURFACE = 0xFF242B34.toInt()
        const val DEFAULT_TEXT = 0xFFFFFFFF.toInt()
        const val DEFAULT_TEXT_SECONDARY = 0xFFD1D5DF.toInt()
        const val DEFAULT_RADIUS_DP = 24f
        const val DEFAULT_GAP_DP = 16f
        const val DEFAULT_FOCUS_SCALE = 1.015f
        const val DEFAULT_TILE_SIZE = 5

        fun defaults() = ThemeState(
            background = DEFAULT_BACKGROUND, panel = DEFAULT_PANEL, surface = DEFAULT_SURFACE,
            text = DEFAULT_TEXT, textSecondary = DEFAULT_TEXT_SECONDARY, accent = DEFAULT_TEXT,
            radiusDp = DEFAULT_RADIUS_DP, gapDp = DEFAULT_GAP_DP,
            focusScale = DEFAULT_FOCUS_SCALE, tileSize = DEFAULT_TILE_SIZE
        )
    }
}

/** Theme file name in the app's files dir. */
internal const val THEME_FILE_NAME = "theme.json"

/** Pure theme parser/validator: String in, ThemeState out, no Context, so it is JVM-testable. */
internal object MatrixTheme {
    const val MAX_BYTES = 16 * 1024

    fun import(filesDir: File, input: java.io.InputStream) {
        val text = BoundedInput.readText(input, MAX_BYTES)
        require(parse(text).ignoredReason == null) { "Invalid theme" }
        BoundedInput.replace(File(filesDir, THEME_FILE_NAME), text)
    }

    /** Parse theme JSON. Any failure ignores the whole file: defaults + the reason. */
    fun parse(json: String): ThemeState {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return ThemeState.defaults().copy(ignoredReason = "Theme exceeds size limit", fromFile = true)
        val parsed = runCatching { JSONObject(json) }.getOrNull()
            ?: return ThemeState.defaults().copy(ignoredReason = "Not valid JSON", fromFile = true)
        return parseObject(parsed)
    }

    internal fun parseObject(json: JSONObject): ThemeState {
        fun fail(reason: String) =
            ThemeState.defaults().copy(ignoredReason = reason, fromFile = true)

        // Each value is parsed once: a present-but-unparseable value fails
        // the file, an absent value falls back to the default below.
        val colours = mutableMapOf<String, Int>()
        for (name in listOf("background", "panel", "surface", "text", "textSecondary", "accent")) {
            if (!json.has(name) || json.isNull(name)) continue
            colours[name] = parseColor(json.optString(name, ""))
                ?: return fail("Colour '$name' must be #RRGGBB or #AARRGGBB")
        }
        val numbers = mutableMapOf<String, Double>()
        for (name in listOf("radius", "gap", "focusScale", "tileSize")) {
            if (!json.has(name) || json.isNull(name)) continue
            val raw = json.opt(name)
            numbers[name] = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                else -> null
            } ?: return fail("'$name' must be a number")
        }

        val background = colours["background"] ?: ThemeState.DEFAULT_BACKGROUND
        val panel = colours["panel"] ?: ThemeState.DEFAULT_PANEL
        val surface = colours["surface"] ?: ThemeState.DEFAULT_SURFACE
        val text = colours["text"] ?: ThemeState.DEFAULT_TEXT
        val textSecondary = colours["textSecondary"] ?: ThemeState.DEFAULT_TEXT_SECONDARY
        val accent = colours["accent"] ?: text
        val radius = numbers["radius"]?.toFloat() ?: ThemeState.DEFAULT_RADIUS_DP
        val gap = numbers["gap"]?.toFloat() ?: ThemeState.DEFAULT_GAP_DP
        val focusScale = numbers["focusScale"]?.toFloat() ?: ThemeState.DEFAULT_FOCUS_SCALE
        val tileSize = numbers["tileSize"]?.toInt() ?: ThemeState.DEFAULT_TILE_SIZE

        if (radius !in 0f..32f) return fail("radius must be 0–32dp (was $radius)")
        if (gap !in 0f..32f) return fail("gap must be 0–32dp (was $gap)")
        if (focusScale !in 1.0f..1.08f) return fail("focusScale must be 1.0–1.08 (was $focusScale)")
        if (tileSize !in 3..6) return fail("tileSize must be 3–6 (was $tileSize)")
        if (contrastRatio(text, background) < 4.5) {
            return fail("text/background contrast must be at least 4.5")
        }
        if (contrastRatio(textSecondary, background) < 3.0) {
            return fail("textSecondary/background contrast must be at least 3.0")
        }
        return ThemeState(background, panel, surface, text, textSecondary, accent,
            radius, gap, focusScale, tileSize, ignoredReason = null, fromFile = true)
    }

    /** Colours must parse as #RRGGBB (opaque) or #AARRGGBB. */
    fun parseColor(raw: String): Int? {
        val hex = raw.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        val value = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) (0xFF000000L or value).toInt() else value.toInt()
    }

    /** WCAG contrast ratio between two ARGB colours. */
    fun contrastRatio(first: Int, second: Int): Double {
        fun luminance(color: Int): Double {
            fun channel(value: Int): Double {
                val c = value / 255.0
                return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel(color shr 16 and 0xFF) +
                0.7152 * channel(color shr 8 and 0xFF) +
                0.0722 * channel(color and 0xFF)
        }
        val lighter = maxOf(luminance(first), luminance(second))
        val darker = minOf(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Load theme.json from the app's files dir; a missing file yields defaults. */
    fun load(filesDir: File): ThemeState {
        val file = File(filesDir, THEME_FILE_NAME)
        if (!file.exists()) return ThemeState.defaults()
        val text = runCatching { file.inputStream().use { BoundedInput.readText(it, MAX_BYTES) } }.getOrNull()
            ?: return ThemeState.defaults().copy(ignoredReason = "theme.json could not be read", fromFile = true)
        return parse(text)
    }

    /** Apply a loaded state as the live theme (layout tokens + colour scheme read it). */
    fun apply(state: ThemeState) {
        MatrixLayout.theme = state
    }

    /** "#AARRGGBB" rendering of a colour, for the Theme page status rows. */
    fun formatColor(color: Int): String = "#%08X".format(color)

    // On-page editing presets (Theme page): tapping a token row advances to the
    // next value, wrapping. Every preset is inside the ranges `parse` accepts,
    // and the colour presets keep text/background contrast above 4.5 and
    // secondary-text/background above 3.0 by construction (near-white text on
    // near-black backgrounds), so a tapped value can never produce an ignored
    // theme file.
    val FOCUS_SCALE_PRESETS = listOf(1.0f, 1.015f, 1.03f, 1.05f, 1.08f)
    val TILE_SIZE_PRESETS = listOf(3, 4, 5, 6)
    val RADIUS_PRESETS = listOf(0f, 8f, 16f, 24f, 32f)
    val GAP_PRESETS = listOf(8f, 12f, 16f, 20f, 24f)
    val BACKGROUND_PRESETS = listOf(0xFF000000.toInt(), 0xFF0B0B10.toInt(), 0xFF101418.toInt(), 0xFF14181D.toInt())
    val PANEL_PRESETS = listOf(0xFF1C1E24.toInt(), 0xFF23262E.toInt(), 0xFF2A2E37.toInt(), 0xFF16181D.toInt())
    val SURFACE_PRESETS = listOf(0xFF242B34.toInt(), 0xFF2E3642.toInt(), 0xFF3A4350.toInt(), 0xFF1E242C.toInt())
    val TEXT_PRESETS = listOf(0xFFFFFFFF.toInt(), 0xFFF5F5F5.toInt(), 0xFFE8E8E8.toInt())
    val TEXT_SECONDARY_PRESETS = listOf(0xFFD1D5DF.toInt(), 0xFFB8BECB.toInt(), 0xFF9AA1AF.toInt())
    val ACCENT_PRESETS = listOf(
        0xFFFFFFFF.toInt(), 0xFF4DA3FF.toInt(), 0xFF4ADE80.toInt(),
        0xFFFBBF24.toInt(), 0xFFF87171.toInt(), 0xFFC084FC.toInt()
    )

    /** Next value in a preset list after `current`, wrapping to the first. */
    fun <T> cycle(values: List<T>, current: T): T {
        val index = values.indexOf(current)
        return values[(index + 1) % values.size]
    }

    /** Next state after tapping one token row: only that token advances. */
    fun nextState(state: ThemeState, token: String): ThemeState = when (token) {
        "background" -> state.copy(background = cycle(BACKGROUND_PRESETS, state.background))
        "panel" -> state.copy(panel = cycle(PANEL_PRESETS, state.panel))
        "surface" -> state.copy(surface = cycle(SURFACE_PRESETS, state.surface))
        "text" -> state.copy(text = cycle(TEXT_PRESETS, state.text))
        "textSecondary" -> state.copy(textSecondary = cycle(TEXT_SECONDARY_PRESETS, state.textSecondary))
        "accent" -> state.copy(accent = cycle(ACCENT_PRESETS, state.accent))
        "radius" -> state.copy(radiusDp = cycle(RADIUS_PRESETS, state.radiusDp))
        "gap" -> state.copy(gapDp = cycle(GAP_PRESETS, state.gapDp))
        "focusScale" -> state.copy(focusScale = cycle(FOCUS_SCALE_PRESETS, state.focusScale))
        "tileSize" -> state.copy(tileSize = cycle(TILE_SIZE_PRESETS, state.tileSize))
        else -> state
    }

    /** theme.json text for a state, with the same ten keys `parse` reads. */
    fun encode(state: ThemeState): String = JSONObject().apply {
        put("background", formatColor(state.background))
        put("panel", formatColor(state.panel))
        put("surface", formatColor(state.surface))
        put("text", formatColor(state.text))
        put("textSecondary", formatColor(state.textSecondary))
        put("accent", formatColor(state.accent))
        put("radius", state.radiusDp.toDouble())
        put("gap", state.gapDp.toDouble())
        put("focusScale", state.focusScale.toDouble())
        put("tileSize", state.tileSize)
    }.toString()

    /** Write a state to theme.json in the app's files dir (IO thread). */
    fun save(filesDir: File, state: ThemeState) {
        val text = encode(state)
        require(parse(text).ignoredReason == null) { "Invalid theme" }
        BoundedInput.replace(File(filesDir, THEME_FILE_NAME), text)
    }
}
