package com.matrix.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The search working indicator: a Siri-style luminous orb.
 *
 * What it looks like: a soft sphere on a dark base, filled with a
 * slowly rotating multicolour swirl (blue, cyan, purple, pink,
 * orange) around a bright white core, finished with a faint rim so
 * it reads on the dark card. The reference is Apple's Siri orb —
 * a spherical, translucent shape with flowing washes of colour and
 * a bright centre — rebuilt fresh in Compose; no Apple asset is
 * copied or embedded.
 *
 * DELIBERATE OVERRIDE, per the user's explicit direction
 * (2026-11-19 21:51 IST: "it should be like apple siri orb"):
 * the multicolour gradient is allowed INSIDE this orb only. The
 * no-ai-slop banned list (gradient blobs, glowing orbs, rainbow
 * ribbons, gradient borders) is otherwise still in force everywhere
 * else in the app. The swirl colours below are Siri identity
 * colours, not Matrix theme tokens, and are named as such — a
 * deliberate redesign choice, not a token that a custom theme
 * should recolour. Everything outside the orb (the quiet progress
 * card, the sentence-case status, the last-4 steps) stays
 * token-quiet.
 *
 * Contained: every layer is drawn inside one clipPath circle, so
 * the swirl and the inner glow never bleed across the card. There
 * is no outer glow and no spill beyond the orb's own radius.
 *
 * Cheap to draw: one Canvas, four draw calls (dark base, rotating
 * sweep-gradient swirl, bright core, thin rim), one slow rotation,
 * no blur layers, no scale or pulse. It only exists while the
 * progress card is shown, so the animation runs only while a
 * search is actually running.
 *
 * Deliberately not the old 150dp showpiece (8 draw layers, 3
 * competing animations) and not the 28dp token ring that replaced
 * it — the user redirected from the ring to the Siri look.
 */

/** One full turn takes 6s: a slow drift, not a spin. */
const val ORB_ROTATION_PERIOD_MS = 6000L

/** The orb is big enough for the swirl to read at 10 feet, never a hero. */
val ORB_SIZE_DP: Dp = 56.dp

/** Thin rim: definition on the dark card, not a border. */
val ORB_RIM_DP: Dp = 1.dp

/** Bright-core alpha at the centre of the highlight. */
const val ORB_CORE_ALPHA = 0.55f

/** Rim alpha: faint, so the orb stays soft. */
const val ORB_RIM_ALPHA = 0.28f

/** Inner-glow alpha at the orb edge: a soft wash, never a halo. */
const val ORB_EDGE_GLOW_ALPHA = 0.10f

/**
 * Containment contract: the glow radius is a fraction of the orb
 * radius and never exceeds it — nothing is drawn outside the orb.
 */
const val ORB_GLOW_RADIUS_FRACTION = 1.0f

/** The bright core covers just over half the orb radius. */
const val ORB_CORE_RADIUS_FRACTION = 0.55f

// Siri identity colours. These are NOT Matrix theme tokens: they are
// the Siri look the user asked for, fixed by design. A custom
// theme.json must not recolour them.
val SIRI_BLUE: Color = Color(0xFF3385FF)
val SIRI_CYAN: Color = Color(0xFF2ED9F2)
val SIRI_PURPLE: Color = Color(0xFF9E4DFF)
val SIRI_PINK: Color = Color(0xFFFF458C)
val SIRI_ORANGE: Color = Color(0xFFFF9E33)

/** Dark base so the swirl reads as luminous, like the real orb. */
val ORB_BASE_CENTER: Color = Color(0xFF1B2340)
val ORB_BASE_EDGE: Color = Color(0xFF0A0E1F)

/**
 * The swirl palette in sweep order. The first colour repeats at the
 * end so the sweep gradient wraps seamlessly with no hard seam.
 * Pure, so the palette contract is unit-testable without composing.
 */
fun orbSwirlColors(): List<Color> = listOf(
    SIRI_BLUE,
    SIRI_CYAN,
    SIRI_PURPLE,
    SIRI_PINK,
    SIRI_ORANGE,
    SIRI_BLUE
)

/** The base disc gradient, centre to edge. */
fun orbBaseColors(): List<Color> = listOf(ORB_BASE_CENTER, ORB_BASE_EDGE)

/**
 * Rotation angle for the single slow swirl: 0 at the start of a
 * turn, 360 at the end, wrapping cleanly. Pure, so the motion
 * contract is unit-testable without composing anything.
 */
fun orbRotationDegrees(elapsedMs: Long): Float {
    val inTurn = Math.floorMod(elapsedMs, ORB_ROTATION_PERIOD_MS)
    return inTurn.toFloat() / ORB_ROTATION_PERIOD_MS.toFloat() * 360f
}

/**
 * Bright-core centre: offset up and left from the orb centre, so
 * the light reads as coming from above, like the real orb.
 * [radiusPx] is the orb radius in pixels.
 */
fun orbCoreCenter(center: Offset, radiusPx: Float): Offset =
    Offset(center.x - radiusPx * 0.25f, center.y - radiusPx * 0.28f)

/** Bright-core radius in pixels for an orb of [radiusPx]. */
fun orbCoreRadius(radiusPx: Float): Float = radiusPx * ORB_CORE_RADIUS_FRACTION

@Composable
fun MatrixSearchOrb(
    modifier: Modifier = Modifier
) {
    // The one motion in this component: a single constant-speed rotation
    // of the sweep-gradient swirl. Nothing else moves.
    val transition = rememberInfiniteTransition(label = "searchOrb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(ORB_ROTATION_PERIOD_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "searchOrbRotation"
    )
    Canvas(modifier = modifier.size(ORB_SIZE_DP)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val circle = Path().apply {
            addOval(Rect(center = center, radius = radius))
        }
        // Every layer is clipped to the orb circle: the swirl and the
        // inner glow live inside the orb's bounds only.
        clipPath(circle) {
            // 1. Dark base disc.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = orbBaseColors(),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // 2. The swirl: a sweep gradient of the Siri palette,
            // rotating once per turn. This is the only moving layer.
            rotate(degrees = rotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = orbSwirlColors(),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    alpha = 0.9f
                )
            }
            // 3. Bright core: a soft white bloom, offset up-left.
            val coreCenter = orbCoreCenter(center, radius)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = ORB_CORE_ALPHA),
                        Color.Transparent
                    ),
                    center = coreCenter,
                    radius = orbCoreRadius(radius)
                ),
                radius = orbCoreRadius(radius),
                center = coreCenter
            )
            // 4. Soft inner wash towards the edge: the gentle glow,
            // kept inside the orb.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = ORB_EDGE_GLOW_ALPHA)
                    ),
                    center = center,
                    radius = radius * ORB_GLOW_RADIUS_FRACTION
                ),
                radius = radius * ORB_GLOW_RADIUS_FRACTION,
                center = center
            )
        }
        // 5. Thin rim, on the clipped edge, for definition on the card.
        drawCircle(
            color = Color.White.copy(alpha = ORB_RIM_ALPHA),
            radius = radius - ORB_RIM_DP.toPx() / 2f,
            center = center,
            style = Stroke(width = ORB_RIM_DP.toPx())
        )
    }
}
