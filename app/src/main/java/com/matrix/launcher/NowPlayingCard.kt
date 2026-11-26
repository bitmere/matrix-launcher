@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.matrix.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/** A single, pinned Home row. Only its background is frosted; content stays sharp. */
@Composable
internal fun NowPlayingCard(item: NowPlayingItem, wallpaper: String?, ownsFocus: Boolean, onFocus: () -> Unit) {
    val session = item.session
    val shape = RoundedCornerShape(MatrixLayout.panelCorner)
    val blue = Color(0xFF4B9FDF)
    val palette = remember { ArtworkPalette.app(blue.toArgb()) }
    var focused by remember { mutableStateOf(false) }
    var focusedCommand by remember { mutableStateOf<NowPlayingCommand?>(null) }
    val cardFocus = remember { FocusRequester() }
    val rotation = rememberMatrixFocusRotation(focused, LocalMatrixCapabilities.current.animations)
    val borderHolder = remember { MatrixGlassBorderHolder() }
    val radius = MatrixLayout.panelCorner
    val spread = MatrixLayout.focusHaloSpread
    val playing = session.status == NowPlayingStatus.Playing
    val stateLabel = stringResource(if (playing) R.string.now_playing_playing else R.string.now_playing_paused)
    val toggleLabel = stringResource(if (playing) R.string.now_playing_pause else R.string.now_playing_play)
    val interaction = remember { MutableInteractionSource() }
    val cardAction = if (session.toggle != null) Modifier.clickable(interactionSource = interaction, indication = null,
        onClickLabel = toggleLabel) { session.toggle?.let { NowPlayingMedia.command(session.id, it) } }
        else Modifier.focusable()
    fun command(value: NowPlayingCommand) = NowPlayingMedia.command(session.id, value)
    Box(Modifier.fillMaxWidth().padding(horizontal = MatrixLayout.channelInset, vertical = MatrixLayout.gap)
        .onFocusChanged { focused = it.hasFocus; if (it.hasFocus) onFocus() }
        .focusProperties { onExit = {
            if (requestedFocusDirection == FocusDirection.Left || requestedFocusDirection == FocusDirection.Right) cancelFocusChange()
        } }.focusGroup()) {
        Spacer(Modifier.matchParentSize().drawBehind {
            if (!focused) return@drawBehind
            borderHolder.resolve(size.width, size.height, 0f, radius.toPx(), .75.dp.toPx(), spread.toPx(), palette)
                ?.let { border -> drawIntoCanvas { border.drawGlow(it.nativeCanvas, rotation.value) } }
        })
        Box(Modifier.matchParentSize().clip(shape)) {
            NowPlayingFrost(wallpaper, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = .72f)))
            Box(Modifier.matchParentSize().background(blue.copy(alpha = .16f)))
            Box(Modifier.matchParentSize().background(MatrixControlStyle.glass(MaterialTheme.colorScheme)))
        }
        Row(Modifier.fillMaxWidth().border(.75.dp, blue.copy(alpha = .45f), shape)
            .padding(MatrixLayout.gap), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MatrixLayout.gap)) {
            Row(Modifier.weight(1f).heightIn(min = MatrixControlStyle.minHeight)
                .focusRequester(cardFocus)
                .onFocusChanged { if (it.isFocused) focusedCommand = null }
                .semantics { stateDescription = stateLabel }
                .then(cardAction), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MatrixLayout.gap)) {
                val context = LocalContext.current
                val artworkRequest = remember(context, item.artwork) {
                    item.artwork?.let { ImageRequest.Builder(context).data(it).size(256).build() }
                }
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(MatrixLayout.cardCorner))
                    .background(MatrixControlStyle.fill(MaterialTheme.colorScheme)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_now_playing), null, Modifier.size(MatrixControlStyle.iconSize))
                    if (artworkRequest != null) AsyncImage(
                        model = artworkRequest,
                        contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(session.source, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stateLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (NowPlayingCommand.Previous in session.commands) MatrixIconButton(R.drawable.ic_now_previous,
                stringResource(R.string.now_playing_previous), { command(NowPlayingCommand.Previous) },
                Modifier.onFocusChanged { if (it.isFocused) focusedCommand = NowPlayingCommand.Previous })
            session.toggle?.let { toggle ->
                MatrixIconButton(if (playing) R.drawable.ic_now_pause else R.drawable.ic_now_play, toggleLabel,
                    { command(toggle) }, Modifier.onFocusChanged { if (it.isFocused) focusedCommand = toggle })
            }
            if (NowPlayingCommand.Next in session.commands) MatrixIconButton(R.drawable.ic_now_next,
                stringResource(R.string.now_playing_next), { command(NowPlayingCommand.Next) },
                Modifier.onFocusChanged { if (it.isFocused) focusedCommand = NowPlayingCommand.Next })
        }
        Spacer(Modifier.matchParentSize().drawBehind {
            if (!focused) return@drawBehind
            borderHolder.resolve(size.width, size.height, 0f, radius.toPx(), .75.dp.toPx(), spread.toPx(), palette)
                ?.let { border -> drawIntoCanvas { border.draw(it.nativeCanvas, rotation.value) } }
        })
    }
    LaunchedEffect(session.commands, session.toggle) {
        val control = focusedCommand
        val removed = when (control) {
            NowPlayingCommand.Previous, NowPlayingCommand.Next -> control !in session.commands
            NowPlayingCommand.Play, NowPlayingCommand.Pause -> session.toggle == null
            null -> false
        }
        if (ownsFocus && removed) {
            withFrameNanos { }
            cardFocus.requestFocus()
        }
    }
}

/** Repaint the same backdrop at the row's coordinates into a card-sized blur layer. */
@Composable
private fun NowPlayingFrost(wallpaper: String?, modifier: Modifier) {
    val backdrop = LocalMatrixBackdrop.current
    val capabilities = LocalMatrixCapabilities.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val backdropSize = backdrop?.size ?: androidx.compose.ui.unit.IntSize.Zero
    val context = LocalContext.current
    val wallpaperRequest = remember(context, wallpaper, capabilities.wallpaper, backdropSize) {
        if (wallpaper != null && capabilities.wallpaper && backdropSize.width > 0 && backdropSize.height > 0) {
            ImageRequest.Builder(context).data(wallpaper)
                .size(backdropSize.width.coerceIn(1, 1920), backdropSize.height.coerceIn(1, 1080)).build()
        } else null
    }
    val painter = rememberAsyncImagePainter(wallpaperRequest, contentScale = ContentScale.Crop)
    val dim = remember(backdropSize.height) { Brush.verticalGradient(listOf(Color.Black.copy(alpha = .65f),
        Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .85f)), endY = backdropSize.height.coerceAtLeast(1).toFloat()) }
    Box(modifier.onGloballyPositioned { coordinates ->
        backdrop?.coordinates?.takeIf { it.isAttached && coordinates.isAttached }?.let {
            offset = it.localPositionOf(coordinates, Offset.Zero)
        }
    }.then(if (capabilities.wallpaper) Modifier.blur(18.dp) else Modifier).drawBehind {
        translate(-offset.x, -offset.y) {
            drawIntoCanvas { backdrop?.draw(it.nativeCanvas) }
            if (wallpaper != null && capabilities.wallpaper) {
                val target = Size(backdropSize.width.toFloat(), backdropSize.height.toFloat())
                val source = painter.intrinsicSize
                if (source.isSpecified && source.width.isFinite() && source.height.isFinite() && source.width > 0 && source.height > 0) {
                    val scale = ContentScale.Crop.computeScaleFactor(source, target)
                    val scaled = Size(source.width * scale.scaleX, source.height * scale.scaleY)
                    translate((target.width - scaled.width) / 2, (target.height - scaled.height) / 2) {
                        with(painter) { draw(scaled) }
                    }
                }
                drawRect(dim, size = target)
            }
        }
    })
}
