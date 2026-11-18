package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.matrix.launcher.core.HomeCardSizing

@Composable
internal fun MatrixAppGrid(page: String, snapshot: HomeSnapshot, repository: ComposeHomeRepository, refresh: (Boolean) -> Unit,
                          back: () -> Unit, reorder: (List<String>) -> Unit, hold: (LaunchApp) -> Unit, open: (LaunchApp) -> Unit) {
    // A cancelled picker must start from persisted choices on the next visit, not a saved old draft.
    var selectedIds by rememberSaveable(page, snapshot.apps.isEmpty()) { mutableStateOf<List<String>>(ArrayList(if (page == "sources") repository.prefs.getStringSet("sources", emptySet()).orEmpty() else snapshot.favorites)) }
    val selected = selectedIds.toSet()
    val first = remember { FocusRequester() }
    var focusedApp by rememberSaveable(page) { mutableStateOf<String?>(null) }
    val ids = remember(snapshot.apps) { snapshot.apps.map { it.id } }
    val restoreIndex = remember(page, ids) { matrixRestoreIndex(ids, focusedApp) }
    val restoreId = ids.getOrNull(restoreIndex)
    val inspection = LocalMatrixUiInspection.current
    val grid = rememberLazyGridState()
    fun orderedSelection() = snapshot.favorites.filter { it in selected } + selected.filter { it !in snapshot.favorites }
    fun save() {
        if (page == "sources") repository.prefs.edit { putStringSet("sources", selected) }
        else repository.saveFavorites(orderedSelection())
        refresh(false)
    }
    BoxWithConstraints {
    val appWidth = MatrixLayout.cardWidth(maxWidth, HomeCardSizing.APP_COLUMNS, 12.dp)
    Column {
        MatrixPageHeader(when(page) { "favorites" -> "Edit favorites"; "sources" -> "App channels on Home"; else -> "All apps" }, back,
            if (snapshot.apps.isEmpty()) first else null) {
            if (page == "favorites") MatrixButton(onClick = { save(); reorder(orderedSelection()) }, role = MatrixControlRole.Commit) { Text("Save & reorder") }
            if (page != "apps") MatrixButton(onClick = { save(); back() }, role = MatrixControlRole.Commit) { Text("Save") }
        }
        MatrixScrollViewport(Modifier.weight(1f)) {
        LazyVerticalGrid(GridCells.Fixed(HomeCardSizing.APP_COLUMNS), state = grid, modifier = Modifier.fillMaxSize().matrixScrollEdges({ grid.canScrollBackward }, { grid.canScrollForward }), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)) {
            items(snapshot.apps, key = { it.id }) { app -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MatrixAppCard(app, repository, appWidth, { _, color -> MatrixAppearance.focusApp(color) }, modifier = (if (app.id == restoreId) Modifier.focusRequester(first) else Modifier).matrixChoice(checked = if (page == "apps") null else app.id in selected)
                    .onFocusChanged { if (it.isFocused) { focusedApp = app.id; inspection?.focusedOption = ids.indexOf(app.id); inspection?.focusedState = "app" }
                        else if (inspection?.focusedOption == ids.indexOf(app.id)) { inspection.focusedOption = -1; inspection.focusedState = "none" } }, onLongClick = { hold(app) }) {
                    if (page == "apps") open(app) else {
                        selectedIds = ArrayList(selected.toMutableSet().apply { if (!add(app.id)) remove(app.id) })
                    }
                }
                if (page != "apps") Text(if (app.id in selected) "✓ On" else "○ Off", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelLarge)
            } }
        }
        }
    }
    LaunchedEffect(page, ids) { if (snapshot.apps.isNotEmpty()) grid.scrollToItem(restoreIndex); withFrameNanos { }; first.requestFocus() }
    }
}

