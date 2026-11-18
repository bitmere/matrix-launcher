package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
internal fun MatrixList(data: Pair<MediaRow, Boolean>?, error: String, page: Int, columns: Int, repository: ComposeHomeRepository, back: () -> Unit,
    home: () -> Unit, next: () -> Unit, resetToken: Int, atFirst: (Boolean) -> Unit, destination: String = "",
    togglePin: ((String) -> Unit)? = null, open: (MediaCard) -> Unit) {
    val initial = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    var focusedCard by rememberSaveable(page, resetToken) { mutableStateOf<String?>(null) }
    val cards = data?.first?.cards.orEmpty()
    val ids = remember(cards) { cards.map { it.id } }
    val resultsVisible = matrixResultsVisible(cards.size, data == null, error)
    val restoreIndex = remember(page, resetToken, ids) { matrixRestoreIndex(ids, focusedCard) }
    val restoreId = ids.getOrNull(restoreIndex)
    val listId = if (destination.startsWith("simkl-list:")) destination.removePrefix("simkl-list:") else null
    val pinned = listId != null && listId in repository.prefs.getStringSet("simkl_pins", emptySet()).orEmpty()
    Column {
        MatrixPageHeader((data?.first?.title ?: "SIMKL list") + " · Page $page", back,
            if (!resultsVisible) initial else null, if (page > 1) "Page one" else "Back") {
            if (listId != null && togglePin != null) {
                MatrixButton(onClick = { togglePin(listId) }, role = if (pinned) MatrixControlRole.Commit else MatrixControlRole.Secondary) {
                    Text(if (pinned) "On Home ✓" else "Add to Home")
                }
            }
            MatrixButton(onClick = home, role = MatrixControlRole.Secondary) { Text("Go to Home") }
        }
        when {
            error.isNotBlank() -> MatrixLoadState(error)
            data == null -> MatrixLoadState("Loading your list…", true)
            data.first.cards.isEmpty() -> MatrixLoadState("No titles in this list yet. Use Back to choose another list.")
            else -> MatrixMediaGrid(data.first.cards, columns, grid, repository, restoreId, initial,
                { index, card -> focusedCard = card.id; atFirst(index == 0) }, open, Modifier.weight(1f)) {
                if (data.second) item(key = "next-page", contentType = "action", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        MatrixButton(onClick = next, role = MatrixControlRole.Secondary) { Text("Next page") }
                    }
                }
            }
        }
    }
    LaunchedEffect(page, resetToken, ids, resultsVisible) { if (resultsVisible) grid.scrollToItem(restoreIndex); withFrameNanos { }; initial.requestFocus() }
}
