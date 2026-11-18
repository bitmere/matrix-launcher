@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.matrix.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

// Apple-style clean geometric typeface (Outfit OFL: light 300, normal 400, medium 500).
private val outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.outfit, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
)

/** Apple-style Outfit scale keeps every TV Material role in the same family and weight hierarchy. */
private fun matrixText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = outfit,
        fontSize = size.sp,
        lineHeight = height.sp,
        fontWeight = weight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

private val matrixTypography = Typography(
    displayLarge = matrixText(76, 84, FontWeight.Light),
    displayMedium = matrixText(56, 64, FontWeight.Light),
    displaySmall = matrixText(48, 56, FontWeight.Light),
    headlineLarge = matrixText(42, 50, FontWeight.Light),
    headlineMedium = matrixText(32, 40),
    headlineSmall = matrixText(26, 34),
    titleLarge = matrixText(22, 28, FontWeight.Medium),
    titleMedium = matrixText(18, 24, FontWeight.Medium),
    titleSmall = matrixText(16, 22, FontWeight.Medium),
    bodyLarge = matrixText(17, 24),
    bodyMedium = matrixText(14, 20),
    bodySmall = matrixText(12, 16),
    labelLarge = matrixText(15, 20, FontWeight.Medium),
    labelMedium = matrixText(12, 16, FontWeight.Medium),
    labelSmall = matrixText(11, 16, FontWeight.Medium)
)

/** TV Material components use the existing Matrix roles, licensed typeface and visual direction. */
@Composable
internal fun MatrixTvTheme(theme: ThemeState = MatrixLayout.theme, content: @Composable () -> Unit) {
    val inset = with(LocalDensity.current) { MatrixLayout.focusInset.toPx() }
    val reveal = remember(inset) { object : BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
            ComposeRevealPolicy.distance(offset, size, containerSize, inset)
    } }
    val background = Color(theme.background)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(theme.accent), onPrimary = Color.Black,
            background = background, onBackground = Color(theme.text),
            surface = Color(theme.panel), onSurface = Color(theme.text),
            surfaceVariant = Color(theme.surface), onSurfaceVariant = Color(theme.textSecondary)
        ),
        typography = matrixTypography
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalBringIntoViewSpec provides reveal, content = content)
    }
}
