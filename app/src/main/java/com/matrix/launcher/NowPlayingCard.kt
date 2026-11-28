@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.matrix.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
import coil.request.ImageRequest

/** A single, pinned Home row. Only its background is frosted; content stays sharp. */
@Composable
internal fun NowPlayingCard(item: NowPlayingItem, ownsFocus: Boolean, onFocus: () -> Unit) {
    val session = item.session
    val shape = RoundedCornerShape(MatrixLayout.panelCorner)
    val ambient = LocalMatrixGlassSource.current?.analysis?.tint ?: MaterialTheme.colorScheme.onSurface.toArgb()
    val palette = remember(ambient) { ArtworkPalette.app(ambient) }
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
        Box(Modifier.matchParentSize().clip(shape).matrixGlassSurface(MatrixGlass.Role.NowPlaying))
        Row(Modifier.fillMaxWidth()
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
