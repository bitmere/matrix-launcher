package com.matrix.launcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin


/** Siri orb visual, split out of MatrixSearchScreen.kt (P0/P1 split); behaviour unchanged. */
@Composable
internal fun SiriGlowingOrb(
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siriOrb")

    // Continuous phase for rolling liquid water flow (0..2π LinearEasing)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterFlowPhase"
    )

    // Gentle organic breathing scale (zero pause at reversal)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    // Soft water refraction core intensity
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlow"
    )

    Canvas(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val baseRadius = (width / 2f) * 0.70f
        val diameter = baseRadius * 2f
        val left = center.x - baseRadius
        val right = center.x + baseRadius
        val bottom = center.y + baseRadius

        // --- Layer 1: Ethereal Water Mist Ambient Bloom (Soft calm aura) ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x3038BDF8).copy(alpha = 0.35f * coreGlow),
                    Color(0x18818CF8).copy(alpha = 0.22f * coreGlow),
                    Color(0x0C2DD4BF).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.50f
            ),
            radius = baseRadius * 1.50f,
            center = center
        )

        // Clip the internal rolling liquid water waves to a smooth circular sphere
        val sphereClip = Path().apply {
            addOval(Rect(left, center.y - baseRadius, right, bottom))
        }

        clipPath(sphereClip) {
            // --- Layer 2: Deep Aquatic Pool Base ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0C4A6E),
                        Color(0xFF0369A1)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // --- Layer 3: Rolling Water Wave 1 (Lagoon Blue / Seafoam) ---
            val wave1Path = Path().apply {
                moveTo(left, bottom)
                var x = left
                val step = 4f
                val baseY = center.y + baseRadius * 0.10f
                val amplitude = baseRadius * 0.14f
                while (x <= right) {
                    val progress = (x - left) / diameter
                    val y = baseY + (amplitude * sin((progress * 2.0 * Math.PI) + phase)).toFloat()
                    lineTo(x, y)
                    x += step
                }
                lineTo(right, bottom)
                close()
            }
            drawPath(
                path = wave1Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x952DD4BF),
                        Color(0x750284C7),
                        Color(0x500F172A)
                    ),
                    startY = center.y - baseRadius * 0.1f,
                    endY = bottom
                )
            )

            // --- Layer 4: Rolling Water Wave 2 (Periwinkle / Soft Lavender liquid current) ---
            val wave2Path = Path().apply {
                moveTo(left, bottom)
                var x = left
                val step = 4f
                val baseY = center.y - baseRadius * 0.08f
                val amplitude = baseRadius * 0.16f
                while (x <= right) {
                    val progress = (x - left) / diameter
                    // Counter-flowing wave with π/2 phase offset
                    val y = baseY + (amplitude * sin((progress * 2.0 * Math.PI) - phase + (Math.PI / 2.0))).toFloat()
                    lineTo(x, y)
                    x += step
                }
                lineTo(right, bottom)
                close()
            }
            drawPath(
                path = wave2Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x88818CF8),
                        Color(0x5038BDF8),
                        Color.Transparent
                    ),
                    startY = center.y - baseRadius * 0.3f,
                    endY = bottom
                )
            )

            // --- Layer 5: Rolling Water Crest 3 (Soft Rosewater / Sky Water Ripple) ---
            val wave3Path = Path().apply {
                moveTo(left, bottom)
                var x = left
                val step = 4f
                val baseY = center.y + baseRadius * 0.22f
                val amplitude = baseRadius * 0.10f
                while (x <= right) {
                    val progress = (x - left) / diameter
                    // Faster harmonic liquid ripple (k=2)
                    val y = baseY + (amplitude * sin((progress * 4.0 * Math.PI) + phase)).toFloat()
                    lineTo(x, y)
                    x += step
                }
                lineTo(right, bottom)
                close()
            }
            drawPath(
                path = wave3Path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x75F472B6),
                        Color(0x4038BDF8),
                        Color.Transparent
                    ),
                    startY = center.y,
                    endY = bottom
                )
            )

            // Delicate shimmering crest line along Wave 2
            val crestPath = Path().apply {
                var x = left
                val step = 4f
                val baseY = center.y - baseRadius * 0.08f
                val amplitude = baseRadius * 0.16f
                var first = true
                while (x <= right) {
                    val progress = (x - left) / diameter
                    val y = baseY + (amplitude * sin((progress * 2.0 * Math.PI) - phase + (Math.PI / 2.0))).toFloat()
                    if (first) { moveTo(x, y); first = false } else { lineTo(x, y) }
                    x += step
                }
            }
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x30E0F2FE),
                        Color(0xB0E0F2FE),
                        Color(0x30E0F2FE)
                    ),
                    startX = left,
                    endX = right
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // --- Layer 6: Luminous Water Refraction Center Bloom ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.88f * coreGlow),
                        Color(0xA038BDF8).copy(alpha = 0.50f * coreGlow),
                        Color(0x30818CF8).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = center
            )
        }

        // --- Layer 7: Micro-thin Glass Meniscus Rim (Delicate 1dp) ---
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0x4038BDF8),
                    Color(0x25818CF8),
                    Color(0x402DD4BF),
                    Color(0x2038BDF8),
                    Color(0x4038BDF8)
                ),
                center = center
            ),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // --- Layer 8: Delicate Top Specular Water Droplet Crescent Sheen ---
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x35FFFFFF),
                    Color(0x0CFFFFFF),
                    Color.Transparent
                ),
                start = Offset(center.x - baseRadius * 0.45f, center.y - baseRadius * 0.60f),
                end = Offset(center.x + baseRadius * 0.25f, center.y - baseRadius * 0.15f)
            ),
            topLeft = Offset(center.x - baseRadius * 0.50f, center.y - baseRadius * 0.65f),
            size = Size(baseRadius * 1.0f, baseRadius * 0.45f)
        )
    }
}
