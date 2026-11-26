@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.matrix.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.matrix.launcher.core.HomeCardSizing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

/** Read by Back's event handler; horizontal movement need not recompose the Home tree. */
private class HomeCardPosition(var index: Int = 0)

@Composable
internal fun MatrixHome(snapshot: HomeSnapshot, loading: Boolean, clock: LocalDateTime, repository: ComposeHomeRepository, active: Boolean,
                       wallpaper: String?, network: NetworkState, homeRequest: Int,
                       settings: () -> Unit, apps: () -> Unit, refresh: () -> Unit, inputs: () -> Unit, wifi: () -> Unit, search: () -> Unit,
                       appClick: (LaunchApp) -> Unit, favoritesEdit: () -> Unit,
                       mediaClick: (MediaCard) -> Unit, mediaHold: (MediaRow, MediaCard) -> Unit, appHold: (LaunchApp) -> Unit) {
    var title by remember { mutableStateOf("Your favorites") }
    val locale = Locale.getDefault()
    val timeFormat = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
    val focusArtwork = rememberMatrixArtworkFocus(repository, active)
    val initialFocus = remember { FocusRequester() }
    // Retain nearby rows across direction changes instead of repeatedly disposing their focus/layout nodes.
    val cacheWindow = remember { LazyLayoutCacheWindow(aheadFraction = .5f, behindFraction = 1f) }
    val list = rememberLazyListState(cacheWindow = cacheWindow)
    val dockCache = remember { LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f) }
    val dockState = rememberLazyListState(cacheWindow = dockCache)
    val scope = rememberCoroutineScope()
    val inspection = LocalMatrixUiInspection.current
    var focusedRow by remember { mutableStateOf("favorites") }
    var focusedFavId by rememberSaveable { mutableStateOf<String?>(null) }
    val favRetained = remember { FocusRequester() }
    val focusedCard = remember { HomeCardPosition() }
    var backRow by remember { mutableStateOf("") }
    var backToken by remember { mutableIntStateOf(0) }
    val revealInset = with(LocalDensity.current) { MatrixLayout.focusInset.toPx() }
    val verticalReveal = remember(revealInset) { object : BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
            if (focusedRow == "favorites" || focusedRow == "header") 0f else ComposeRevealPolicy.distance(offset, size, containerSize, revealInset)
    } }
    // The framework reveals channels; one cancellable animation owns returning to the top group.
    LaunchedEffect(list) {
        // Observe focus here so changing channels does not invalidate the whole Home composition.
        snapshotFlow { focusedRow }.collectLatest { row ->
            if ((row == "favorites" || row == "header") && (list.firstVisibleItemIndex != 0 || list.firstVisibleItemScrollOffset != 0)) list.animateScrollToItem(0)
        }
    }
    DisposableEffect(inspection, list) {
        inspection?.homeColumn = list
        onDispose { inspection?.homeColumn = null }
    }
    val favorites = remember(snapshot.favorites, snapshot.apps) {
        snapshot.favorites.mapNotNull { id -> snapshot.apps.find { it.id == id } }
    }
    BackHandler(enabled = active) {
        if (focusedRow != "favorites" && focusedRow != "header" && focusedCard.index > 0) { backRow = focusedRow; backToken++ }
        else if (focusedRow == "favorites" && focusedCard.index > 0) {
            focusedFavId = favorites.firstOrNull()?.id
            scope.launch { dockState.scrollToItem(0); withFrameNanos { }; initialFocus.requestFocus() }
        }
        else {
            focusedFavId = favorites.firstOrNull()?.id
            scope.launch { list.scrollToItem(0); dockState.scrollToItem(0); withFrameNanos { }; initialFocus.requestFocus() }
        }
    }
    val nowPlaying = snapshot.rows.firstOrNull { it.id == MatrixPrefs.NOW_PLAYING_ROW }?.nowPlaying
    val contentRows = snapshot.rows.filterNot { it.id == MatrixPrefs.NOW_PLAYING_ROW }
    LaunchedEffect(nowPlaying != null) {
        if (nowPlaying == null && focusedRow == MatrixPrefs.NOW_PLAYING_ROW && active) {
            list.scrollToItem(0)
            withFrameNanos { }
            initialFocus.requestFocus()
        }
    }
    val columns = HomeCardSizing.dockColumns(favorites.size)
    val mediaColumns = HomeCardSizing.contentColumns(MatrixSafeMode.effectiveColumns(repository.prefs, repository.safeMode))
    MatrixBackdropSurface(artworkBackdrop = wallpaper == null) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        if (wallpaper != null) {
            // Decode at most 1920x1080: a 4K TV must not allocate a full-screen
            // backdrop bitmap. Smaller screens decode at view size, as before.
            val density = LocalDensity.current
            val wallpaperWidth = minOf((maxWidth.value * density.density).roundToInt(), 1920)
            val wallpaperHeight = minOf((maxHeight.value * density.density).roundToInt(), 1080)
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(wallpaper).size(wallpaperWidth, wallpaperHeight).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .65f), Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .85f)))))
        }
        val dockWidth = MatrixLayout.cardWidth(maxWidth, columns, 46.dp + MatrixLayout.dockInset)
        val mediaWidth = MatrixLayout.cardWidth(maxWidth, mediaColumns, MatrixLayout.channelInset)
        CompositionLocalProvider(LocalBringIntoViewSpec provides verticalReveal) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            val vertical = event.key == Key.DirectionDown || event.key == Key.DirectionUp
            if (vertical && event.type == KeyEventType.KeyDown && inspection != null) {
                inspection.navigationDowns++
                if (event.nativeKeyEvent.repeatCount > 0) inspection.navigationRepeats++
            }
            val deferred = vertical && event.type == KeyEventType.KeyDown &&
                ComposeRevealPolicy.deferRepeat(event.nativeKeyEvent.repeatCount, list.isScrollInProgress)
            if (deferred) inspection?.deferredRepeats = (inspection?.deferredRepeats ?: 0) + 1
            deferred
        }, contentPadding = PaddingValues(vertical = 34.dp)) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("WELCOME HOME", style = MaterialTheme.typography.labelLarge)
                        Text(clock.format(timeFormat), style = MaterialTheme.typography.displayMedium)
                        Text(clock.format(dateFormat).uppercase(locale), style = MaterialTheme.typography.bodyMedium) }
                    Row(Modifier.focusProperties {
                        onExit = {
                            if (requestedFocusDirection == FocusDirection.Left ||
                                requestedFocusDirection == FocusDirection.Right ||
                                requestedFocusDirection == FocusDirection.Up) cancelFocusChange()
                        }
                    }.focusGroup().onFocusChanged { if (it.hasFocus) { focusedRow = "header"; focusedCard.index = 0; inspection?.homeRow = -1; inspection?.homeCard = -1 } }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (repository.prefs.getBoolean("home_search_enabled", true)) {
                            Tool(R.drawable.ic_search, "Search", search)
                        }
                        Tool(R.drawable.ic_input, "Inputs", inputs, if (favorites.isEmpty()) Modifier.focusRequester(initialFocus) else Modifier)
                        Tool(if (network.ethernet) R.drawable.ic_ethernet else R.drawable.ic_wifi, if (network.connected) "Connected" else "Offline", wifi, connected = network.connected)
                        Tool(R.drawable.ic_home_refresh, if (loading) "Loading" else "Refresh", { if (!loading) refresh() })
                        Tool(R.drawable.ic_apps, "All apps", apps); Tool(R.drawable.ic_settings, "Settings", settings)
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
            if (nowPlaying != null) item(key = MatrixPrefs.NOW_PLAYING_ROW, contentType = "now-playing") {
                NowPlayingCard(nowPlaying, wallpaper, focusedRow == MatrixPrefs.NOW_PLAYING_ROW) {
                    focusedRow = MatrixPrefs.NOW_PLAYING_ROW
                    focusedCard.index = 0
                    inspection?.homeRow = -2
                    inspection?.homeCard = 0
                }
            }
            item(key = "favorites-title") {
                Column(Modifier.padding(horizontal = 55.dp)) { Text("FAVORITES · HOLD OK TO EDIT", style = MaterialTheme.typography.labelMedium)
                    Text(title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)) }
            }
            item(key = "favorites") {
                val favTargetId = if (focusedFavId != null && (favorites.any { it.id == focusedFavId } || focusedFavId == "edit-favorites")) {
                    focusedFavId!!
                } else {
                    favorites.firstOrNull()?.id ?: "edit-favorites"
                }
                Box(Modifier.padding(horizontal = 46.dp).clip(RoundedCornerShape(28.dp))
                    .background(MatrixControlStyle.glass(MaterialTheme.colorScheme))) {
                    MatrixFocusRow(dockState, MatrixLayout.dockInset, dockWidth, "favorites", availableWidth = availableWidth - 46.dp * 2,
                        columns = columns, verticalInset = MatrixLayout.dockInset,
                        fallback = favRetained,
                        isLastRow = contentRows.isEmpty()) { slotWidth ->
                        itemsIndexed(favorites, key = { _, app -> app.id }, contentType = { _, _ -> "app" }) { index, app ->
                            val isTarget = app.id == favTargetId
                            MatrixAppCard(app, repository, slotWidth,
                                { name, color ->
                                    focusedRow = "favorites"
                                    focusedCard.index = index
                                    focusedFavId = app.id
                                    inspection?.homeRow = 0
                                    inspection?.homeCard = index
                                    focusArtwork("")
                                    title = name
                                    MatrixAppearance.focusApp(color)
                                },
                                modifier = (if (app == favorites.firstOrNull()) Modifier.focusRequester(initialFocus) else Modifier)
                                    .then(if (isTarget) Modifier.focusRequester(favRetained) else Modifier),
                                onLongClick = { appHold(app) }, showTitle = false) { appClick(app) } }
                        item(key = "edit-favorites", contentType = "action") {
                            val isTarget = "edit-favorites" == favTargetId
                            MatrixActionCard("Edit favorites", R.drawable.ic_edit_favorites, slotWidth, favoritesEdit,
                                modifier = if (isTarget) Modifier.focusRequester(favRetained) else Modifier) {
                                focusedRow = "favorites"
                                focusedCard.index = favorites.size
                                focusedFavId = "edit-favorites"
                                inspection?.homeRow = 0
                                inspection?.homeCard = favorites.size
                                title = "Edit favorites"
                                focusArtwork("")
                            }
                        }
                    }
                }
            }
            if (contentRows.isEmpty()) item(key = "status") { Text(snapshot.status, Modifier.padding(48.dp), style = MaterialTheme.typography.bodyMedium) }
            itemsIndexed(contentRows, key = { _, row -> row.id }, contentType = { _, _ -> "channel" }) { rowIndex, row ->
                MatrixChannelRow(row, mediaWidth, availableWidth, mediaColumns, "channel-${rowIndex + 1}", backRow, backToken,
                    isLastRow = rowIndex == contentRows.lastIndex,
                    onFocus = { index, card -> focusedRow = row.id; focusedCard.index = index; inspection?.homeRow = rowIndex + 1; inspection?.homeCard = index; focusArtwork(card.image) },
                    click = mediaClick, hold = { mediaHold(row, it) }, backHandled = { backRow = "" })

            }
        }
        }
    }
    }
    LaunchedEffect(favorites.isNotEmpty(), homeRequest) { if (active) {
        focusedFavId = favorites.firstOrNull()?.id
        list.scrollToItem(0); dockState.scrollToItem(0)
        withFrameNanos { }; initialFocus.requestFocus()
    } }
}

@Composable
private fun MatrixChannelRow(row: MediaRow, width: androidx.compose.ui.unit.Dp, availableWidth: androidx.compose.ui.unit.Dp, columns: Int, rowKey: String, backRow: String, backToken: Int,
                             isLastRow: Boolean = false,
                             onFocus: (Int, MediaCard) -> Unit, click: (MediaCard) -> Unit, hold: (MediaCard) -> Unit, backHandled: () -> Unit) {
    val portraitOnly = remember(row.cards) { row.cards.isNotEmpty() && row.cards.all { it.portrait } }
    val preferred = MatrixLayout.mediaWidth(width, portraitOnly, availableWidth, columns)
    val rowCache = remember { LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f) }
    val state = rememberLazyListState(cacheWindow = rowCache)
    val first = remember { FocusRequester() }
    val retained = remember { FocusRequester() }
    var focusedId by rememberSaveable(row.id) { mutableStateOf<String?>(null) }
    val ids = remember(row.cards) { row.cards.map { it.id } }
    val retainedId = ids.getOrNull(matrixRestoreIndex(ids, focusedId))
    Column {
        Text(row.title, Modifier.padding(start = 55.dp, top = 30.dp, bottom = 12.dp), style = MaterialTheme.typography.titleLarge)
        MatrixFocusRow(state, MatrixLayout.channelInset, preferred, rowKey, availableWidth, columns = if (portraitOnly) null else columns,
            fallback = if (ids.isNotEmpty()) retained else FocusRequester.Default,
            isLastRow = isLastRow) { slotWidth ->
            itemsIndexed(row.cards, key = { _, card -> card.id }, contentType = { _, _ -> "media" }) { index, card ->
                val isRetained = card.id == retainedId
                MatrixMediaCard(card, slotWidth, { focusedId = card.id; onFocus(index, card) },
                    modifier = (if (index == 0) Modifier.focusRequester(first) else Modifier).then(if (isRetained) Modifier.focusRequester(retained) else Modifier),
                    artworkWidth = if (portraitOnly) slotWidth else MatrixLayout.mediaWidth(slotWidth, card.portrait, availableWidth, columns), onLongClick = { hold(card) }) { click(card) } }
        }
    }
    LaunchedEffect(backToken) { if (backToken > 0 && backRow == row.id && row.cards.isNotEmpty()) {
        focusedId = row.cards.firstOrNull()?.id
        state.scrollToItem(0); withFrameNanos { }; first.requestFocus()
        // A reconstructed lazy row must not replay an already-consumed Back request.
        backHandled()
    } }
}

@Composable
private fun Tool(icon: Int, label: String, click: () -> Unit, modifier: Modifier = Modifier, connected: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(MatrixControlStyle.toolbarSlotWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        MatrixIconButton(icon, label, click, modifier = modifier.onFocusChanged { focused = it.isFocused },
            contentAlpha = if (connected || focused) 1f else .85f)
        Box(Modifier.height(25.dp).padding(top = 5.dp)) {
            if (focused) Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

