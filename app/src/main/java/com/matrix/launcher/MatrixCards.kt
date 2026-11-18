package com.matrix.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlin.math.roundToInt
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Geometry only: reused decoded posters should not restart with an incorrect fitted rim. */
private val artworkAspects = android.util.LruCache<String, Float>(128)

/** Seed from the same bounded memory cache; only a miss schedules the shared IO loader. */
@Composable
private fun rememberMatrixBanner(pkg: String?, repository: ComposeHomeRepository): State<Pair<android.graphics.Bitmap, Int>?> {
    val cached = remember(repository, pkg) { repository.cachedBanner(pkg) }
    return produceState(cached, repository, pkg) {
        value = cached
        if (value == null && pkg != null) value = repository.banner(pkg)
    }
}

@Composable
private fun Modifier.tvCardClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
): Modifier {
    if (onLongClick == null) return this
    val scope = rememberCoroutineScope()
    var pressJob by remember { mutableStateOf<Job?>(null) }
    var isLongPressTriggered by remember { mutableStateOf(false) }
    return this
        .onFocusChanged {
            if (!it.isFocused && !isLongPressTriggered) {
                pressJob?.cancel()
                pressJob = null
            }
        }
        .onPreviewKeyEvent { event ->
            val native = event.nativeKeyEvent
            val isSelect = native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                native.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                native.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                native.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A
            if (isSelect) {
                when (native.action) {
                    android.view.KeyEvent.ACTION_DOWN -> {
                        if (native.repeatCount == 0 && pressJob == null) {
                            isLongPressTriggered = false
                            pressJob = scope.launch {
                                delay(380L)
                                isLongPressTriggered = true
                                onLongClick()
                            }
                        }
                        true
                    }
                    android.view.KeyEvent.ACTION_UP -> {
                        pressJob?.cancel()
                        pressJob = null
                        if (isLongPressTriggered) {
                            isLongPressTriggered = false
                        } else {
                            onClick()
                        }
                        true
                    }
                    else -> false
                }
            } else false
        }
}

internal object MatrixCardDefaults {
    val artworkShape get() = RoundedCornerShape(MatrixLayout.cardCorner)
    @Composable
    fun shape() = CardDefaults.shape(RectangleShape)
    @Composable
    fun border() = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None)
    @Composable
    fun scale() = CardDefaults.scale(focusedScale = MatrixLayout.cardFocusScale)
    @Composable
    fun colors() = CardDefaults.colors(
        containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = Color.Transparent, focusedContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun MatrixAppCard(app: LaunchApp, repository: ComposeHomeRepository, width: Dp,
                           onFocus: (String, Int) -> Unit, modifier: Modifier = Modifier,
                           onLongClick: (() -> Unit)? = null, showTitle: Boolean = true, onClick: () -> Unit) {
    val art by rememberMatrixBanner(app.id, repository)
    val fallback = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val palette = remember(art?.second, fallback) { ArtworkPalette.app(art?.second ?: fallback) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(art) { if (focused) onFocus(app.title, art?.second ?: fallback) }
    ClassicCard(
        onClick = onClick, onLongClick = null,
        modifier = modifier.tvCardClickable(onClick, onLongClick).width(width).then(if (!showTitle) Modifier.semantics { contentDescription = app.title } else Modifier).onFocusChanged {
            focused = it.isFocused; if (it.isFocused) onFocus(app.title, art?.second ?: fallback)
        },
        colors = MatrixCardDefaults.colors(), shape = MatrixCardDefaults.shape(), border = MatrixCardDefaults.border(),
        scale = MatrixCardDefaults.scale(),
        image = {
            MatrixArtwork(16f / 9f, focused, palette = palette) {
                if (art != null) Image(art!!.first.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Image(painterResource(R.drawable.ic_apps), null, Modifier.size(32.dp))
            }
        },
        title = { if (showTitle) Text(app.title, Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    )
}

@Composable
internal fun MatrixActionCard(title: String, icon: Int, width: Dp, click: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}) {
    var focused by remember { mutableStateOf(false) }
    ClassicCard(onClick = click, modifier = modifier.width(width).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }, scale = MatrixCardDefaults.scale(),
        image = { MatrixArtwork(16f / 9f, focused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(icon), null, Modifier.size(40.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            }
        } }, title = {}, colors = MatrixCardDefaults.colors(), shape = MatrixCardDefaults.shape(), border = MatrixCardDefaults.border())
}

@Composable
internal fun MatrixMiniArtwork(app: LaunchApp?, card: MediaCard?, repository: ComposeHomeRepository, icon: Int? = null) {
    val banner by rememberMatrixBanner(app?.id, repository)
    Box(Modifier.size(64.dp, 40.dp).clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        when {
            icon != null -> androidx.tv.material3.Icon(painterResource(icon), null, Modifier.size(28.dp))
            banner != null -> Image(banner!!.first.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            !card?.image.isNullOrBlank() -> AsyncImage(card?.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else -> Image(painterResource(R.drawable.ic_apps), null, Modifier.size(28.dp))
        }
    }
}

/**
 * Primary TV Material 3 media card for Home channels, lists, and search grids.
 *
 * Artwork features:
 * - Proportional aspect ratio fitting (portrait 2:3 or landscape 16:9).
 * - Focused outward glass glow shader.
 * - Top-start media type badge ("AI Pick", "Movie", "Series").
 * - Top-end rating overlay badge ("★ 8.7", "★ 7.6") or "UPCOMING" indicator.
 * - Bottom-center quiet translucent playback progress bar.
 * - Dual-line title, clean formatted metadata, and optional AI recommendation reason.
 */
@Composable
internal fun MatrixMediaCard(card: MediaCard, width: Dp, onFocus: (String) -> Unit,
                             modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null, artworkWidth: Dp = width, onClick: () -> Unit) {
    val progress = if (card.duration > 0 && card.position > 0)
        (card.position.toDouble() / card.duration).coerceIn(0.0, 1.0).toFloat() else null
    val density = androidx.compose.ui.platform.LocalDensity.current
    val (imageWidth, imageHeight) = remember(density, artworkWidth, card.portrait) {
        with(density) {
            val w = artworkWidth.roundToPx()
            val h = (w * if (card.portrait) 1.5f else 9f / 16f).toInt()
            w to h
        }
    }
    var focused by remember { mutableStateOf(false) }
    val frameAspect = if (card.portrait) 2f / 3f else 16f / 9f
    var sourceAspect by remember(card.image) { mutableFloatStateOf(artworkAspects.get(card.image) ?: frameAspect) }
    val context = LocalContext.current
    val request = remember(context, card.image, imageWidth, imageHeight) {
        ImageRequest.Builder(context).data(card.image).size(imageWidth, imageHeight).crossfade(false).allowRgb565(true).build()
    }
    ClassicCard(
        onClick = onClick, onLongClick = null,
        modifier = modifier.tvCardClickable(onClick, onLongClick).width(width).semantics {
            if (progress != null) stateDescription = "${(progress * 100).roundToInt()}% watched"
        }.onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus(card.id) },
        colors = MatrixCardDefaults.colors(), shape = MatrixCardDefaults.shape(), border = MatrixCardDefaults.border(),
        scale = MatrixCardDefaults.scale(),
        image = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(artworkWidth)) {
            MatrixArtwork(frameAspect, focused, sourceAspect) {
                if (card.image.isNotBlank()) AsyncImage(
                    model = request,
                    contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                    onSuccess = {
                        val aspect = ArtworkFit.aspect(it.result.drawable.intrinsicWidth, it.result.drawable.intrinsicHeight, frameAspect)
                        artworkAspects.put(card.image, aspect)
                        sourceAspect = aspect
                    }
                ) else Image(painterResource(R.drawable.ic_apps), null, Modifier.align(Alignment.Center).size(32.dp))
                if (card.mediaType.isNotBlank()) Text(card.mediaType, Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), MatrixControlStyle.shape)
                    .padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                val ratingBadge = remember(card.metadata) { extractRatingBadge(card.metadata) }
                val isUpcoming = remember(card.metadata, ratingBadge) {
                    ratingBadge == null && (
                        card.metadata.contains("Upcoming", ignoreCase = true) ||
                            card.metadata.trim().let { m ->
                                val y = m.filter(Char::isDigit).take(4).toIntOrNull()
                                y != null && y >= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                            }
                    )
                }
                if (ratingBadge != null) {
                    Text(
                        text = ratingBadge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xEE111524), MatrixControlStyle.shape)
                            .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.75f), MatrixControlStyle.shape)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD166),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                } else if (isUpcoming) {
                    Text(
                        text = "UPCOMING",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xEE0F172A), MatrixControlStyle.shape)
                            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), MatrixControlStyle.shape)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                if (progress != null) {
                    MatrixPlaybackProgress(progress, Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp, vertical = 7.dp).fillMaxWidth())
                }
            }
            }
            }
        },
        title = { Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(card.title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val metaText = remember(card.metadata) { card.metadata.replace("\n", " · ").trim() }
            if (metaText.isNotBlank()) Text(metaText, Modifier.fillMaxWidth().padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (card.mediaType.isNotBlank()) Text(card.summary, Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } }
    )
}

/** A quiet translucent track keeps the artwork visible without a second outline. */
@Composable
private fun MatrixPlaybackProgress(progress: Float, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = .22f)
    val indicator = MaterialTheme.colorScheme.onSurface.copy(alpha = .94f)
    Canvas(modifier.height(MatrixLayout.playbackTrackHeight).progressSemantics(progress)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        if (progress > 0f) drawRoundRect(indicator,
            size = Size(size.width * progress, size.height), cornerRadius = radius)
    }
}

/** Keep layout stable, but fit the decorated surface to the decoded image itself. */
@Composable
private fun MatrixArtwork(ratio: Float, focused: Boolean, sourceAspect: Float = ratio, palette: ArtworkPalette? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = MatrixCardDefaults.artworkShape
    val outline = animateFloatAsState(if (focused) 1f else 0f, tween(MatrixLayout.focusFadeMillis, easing = FastOutSlowInEasing), label = "artwork focus")
    val rotation = rememberMatrixFocusRotation(focused, LocalMatrixCapabilities.current.animations)
    val color = MaterialTheme.colorScheme.onSurface
    // App cards use their own cached accent; focused media uses the existing sampled palette.
    val glowPalette = palette ?: (if (focused) MatrixAppearance.palette else null) ?: ArtworkPalette.app(color.toArgb())
    // Fit during measurement, not subcomposition: every recycled card uses this hot path.
    Box(Modifier.fillMaxWidth().aspectRatio(ratio)
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val inset = MatrixLayout.artworkInset.toPx() * 2
                val bounds = ArtworkFit.bounds(width - inset, height - inset, sourceAspect)
                val artwork = measurable.measure(Constraints.fixed(
                    bounds.width.roundToInt().coerceIn(0, width), bounds.height.roundToInt().coerceIn(0, height)))
                layout(width, height) { artwork.placeRelative((width - artwork.width) / 2, (height - artwork.height) / 2) }
            }
            .drawWithCache {
                val offset = MatrixLayout.focusOutlineOffset.toPx()
                val radius = MatrixLayout.cardCorner.toPx() + offset
                val stroke = MatrixLayout.focusOutlineWidth.toPx()
                val spread = MatrixLayout.focusHaloSpread.toPx()
                val origin = Offset(-offset, -offset)
                val extent = Size(size.width + offset * 2, size.height + offset * 2)
                var reflection: MatrixGlassBorder? = null
                var lastPalette: ArtworkPalette? = null
                onDrawWithContent {
                    val alpha = outline.value
                    if (alpha > 0f) {
                        if (reflection == null || lastPalette != glowPalette) {
                            lastPalette = glowPalette
                            reflection = MatrixGlassBorder(android.graphics.RectF(origin.x, origin.y,
                                origin.x + extent.width, origin.y + extent.height), radius, stroke, glowPalette, spread)
                        }
                        drawIntoCanvas { reflection?.drawGlow(it.nativeCanvas, rotation.value, alpha) }
                    }
                    drawContent()
                    if (alpha > 0f) {
                        drawIntoCanvas { reflection?.draw(it.nativeCanvas, rotation.value, alpha) }
                    }
                }
            }, contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center, content = content)
    }
}

private val RATING_IMDB_REGEX = Regex("""IMDb[:\s]+([\d.]+)""", RegexOption.IGNORE_CASE)
private val RATING_TMDB_REGEX = Regex("""TMDB[:\s]+([\d.]+)""", RegexOption.IGNORE_CASE)
private val RATING_SIMKL_REGEX = Regex("""SIMKL[:\s]+([\d.]+)""", RegexOption.IGNORE_CASE)
private val RATING_STAR_REGEX = Regex("""[★*]\s*([\d.]+)""")
private val RATING_SLASH_REGEX = Regex("""\b(10(?:\.0)?|[1-9](?:\.\d)?)\s*/\s*10\b""")

/**
 * Extracts IMDb, TMDB, or SIMKL ratings formatted as a star badge (e.g. "★ 8.7").
 *
 * Checks in priority order:
 * 1. IMDb score (e.g. "IMDb 8.7", "1999 · IMDb 8.7") -> "★ 8.7"
 * 2. TMDB score (e.g. "TMDB 7.6", "2022\nTMDB 7.6") -> "★ 7.6"
 * 3. SIMKL score (e.g. "SIMKL 8.1", "2022 · SIMKL 8.1") -> "★ 8.1"
 * 4. Generic star glyph (e.g. "★ 8.4") -> "★ 8.4"
 * 5. Fractional score (e.g. "8.5/10") -> "★ 8.5"
 *
 * @param metadata Raw card metadata string.
 * @return Formatted badge string with unicode star glyph, or null if no valid rating is found.
 */
internal fun extractRatingBadge(metadata: String): String? {
    if (metadata.isBlank()) return null
    val imdbMatch = RATING_IMDB_REGEX.find(metadata)
    if (imdbMatch != null) return "★ " + imdbMatch.groupValues[1]
    val tmdbMatch = RATING_TMDB_REGEX.find(metadata)
    if (tmdbMatch != null) return "★ " + tmdbMatch.groupValues[1]
    val simklMatch = RATING_SIMKL_REGEX.find(metadata)
    if (simklMatch != null) return "★ " + simklMatch.groupValues[1]
    val starMatch = RATING_STAR_REGEX.find(metadata)
    if (starMatch != null) return "★ " + starMatch.groupValues[1]
    val slashMatch = RATING_SLASH_REGEX.find(metadata)
    if (slashMatch != null) return "★ " + slashMatch.groupValues[1]
    return null
}
