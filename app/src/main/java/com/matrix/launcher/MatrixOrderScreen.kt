@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.matrix.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.matrix.launcher.core.ReorderSession

/**
 * Pure visibility-toggle decision for an order row, so the choice is JVM-testable.
 * Returns the new hidden set and whether the row is shown on Home afterwards.
 */
internal fun toggleOrderRowHidden(hidden: Set<String>, id: String): Pair<Set<String>, Boolean> {
    val next = hidden.toMutableSet().apply { if (!add(id)) remove(id) }
    return next to (id !in next)
}

/** Movement follows stable identities; every list mutation publishes an observable snapshot. */
@Composable
internal fun MatrixOrder(page: OrderPage, repository: ComposeHomeRepository, refresh: (Boolean) -> Unit, back: () -> Unit) {
    val draft = remember(page) { ReorderSession(page.entries) }
    var entries by remember(page) { mutableStateOf(page.entries.toList()) }
    var movingId by remember(page) { mutableStateOf<String?>(null) }
    var focusId by remember(page) { mutableStateOf(page.entries.firstOrNull { it.id == page.selected }?.id ?: page.entries.firstOrNull()?.id.orEmpty()) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(repository.prefs.getStringSet("hidden_rows", emptySet()).orEmpty().toSet()) }
    val list = rememberLazyListState()
    val targets = remember(page) { page.entries.associate { it.id to FocusRequester() } }
    val reveals = remember(page) { page.entries.associate { it.id to BringIntoViewRequester() } }
    val saveFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    fun publish(id: String) {
        // Reordering must not anchor the viewport to the old first visible item's key.
        if (draft.items != entries) list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        entries = draft.items.toList()
        movingId = draft.items.getOrNull(draft.moving)?.id
        focusId = id; focusRequest++
    }
    fun begin(id: String) {
        draft.begin(entries.indexOfFirst { it.id == id }); status = ""; publish(id)
    }
    fun save() {
        val id = movingId ?: focusId
        draft.confirm()
        repository.saveOrder(page.preference, draft.items.map { it.id })
        refresh(false); status = "Order saved"; publish(id)
    }
    fun cancelOrBack() {
        val id = movingId
        if (id == null) back() else { draft.cancel(); status = "Move cancelled"; publish(id) }
    }
    BackHandler { cancelOrBack() }
    Column(Modifier.onPreviewKeyEvent { event ->
        if (movingId == null) false else when (event.key) {
            Key.DirectionUp, Key.DirectionDown -> {
                if (event.type == KeyEventType.KeyDown && draft.move(if (event.key == Key.DirectionDown) 1 else -1)) publish(movingId!!)
                true
            }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.ButtonA -> { if (event.type == KeyEventType.KeyUp) save(); true }
            Key.DirectionRight -> { if (event.type == KeyEventType.KeyDown) saveFocus.requestFocus(); true }
            Key.DirectionLeft -> { if (event.type == KeyEventType.KeyDown) focusRequest++; true }
            else -> false
        }
    }) {
        MatrixPageHeader(page.title, ::cancelOrBack, if (entries.isEmpty()) backFocus else null,
            if (movingId != null) "Cancel move" else "Back") {
            if (entries.isNotEmpty()) MatrixButton(onClick = ::save, modifier = Modifier.focusRequester(saveFocus), role = MatrixControlRole.Commit) { Text("Save order") }
        }
        val position = entries.indexOfFirst { it.id == movingId }
        Text(when {
            entries.isEmpty() -> "No published items yet. Choose source apps first."
            position >= 0 -> "Moving ${entries[position].title} · ${position + 1} of ${entries.size}\n↑/↓ moves · OK saves · Back cancels · → Save order"
            else -> (if (status.isBlank()) "" else "$status\n") + "OK picks up · Save order keeps changes" + if (page.visibility) "\nHold OK to show or hide" else ""
        }, Modifier.heightIn(min = 68.dp).padding(bottom = MatrixLayout.gap), style = MaterialTheme.typography.bodyMedium)
        MatrixScrollViewport(Modifier.weight(1f)) {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().matrixScrollEdges({ list.canScrollBackward }, { list.canScrollForward }),
                contentPadding = PaddingValues(MatrixLayout.focusInset), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    // One explicit press lifecycle, shared with the home cards: the
                    // modifier times holds with a 380 ms timer on the initial DOWN
                    // instead of depending on key repeats (many remotes send none),
                    // fires a short press at once, and suppresses it after a hold.
                    // The Button's own onLongClick stays null so the built-in
                    // repeat-dependent long-press cannot double-fire.
                    MatrixButton(onClick = { begin(entry.id) }, role = MatrixControlRole.Option,
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(reveals.getValue(entry.id)).focusRequester(targets.getValue(entry.id))
                            .onFocusChanged { if (it.isFocused && movingId == null) focusId = entry.id }
                            .tvCardClickable(
                                onClick = { begin(entry.id) },
                                onLongClick = if (!page.visibility || movingId != null) null else { {
                                    val (nextHidden, shown) = toggleOrderRowHidden(hidden, entry.id)
                                    hidden = nextHidden
                                    repository.rowShown(entry.id, shown); refresh(false)
                                } }
                            )) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                                if (movingId == entry.id) Icon(painterResource(R.drawable.ic_matrix_move), "Moving item", Modifier.size(22.dp))
                                else Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                            }
                            MatrixMiniArtwork(entry.app, entry.media, repository, entry.icon)
                            MatrixRowText(entry.title, when {
                                movingId == entry.id -> "Moving · position ${index + 1} of ${entries.size}"
                                page.visibility -> if (entry.id in hidden) "Hidden on Home" else "Shown on Home"
                                else -> ""
                            }, Modifier.weight(1f))
                            if (page.visibility) {
                                Text(if (entry.id in hidden) "○" else "✓", style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.clearAndSetSemantics { })
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(page, focusRequest) {
        withFrameNanos { }
        val index = entries.indexOfFirst { it.id == focusId }
        if (index < 0) { withFrameNanos { }; backFocus.requestFocus() }
        else {
            if (list.layoutInfo.visibleItemsInfo.none { it.key == focusId }) list.scrollToItem(index)
            withFrameNanos { }; targets.getValue(focusId).requestFocus()
            reveals.getValue(focusId).bringIntoView()
        }
    }
}
