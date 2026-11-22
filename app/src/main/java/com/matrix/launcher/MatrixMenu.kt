package com.matrix.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** State and identity travel with each row instead of parallel lists or decorated labels. */
@Immutable
internal data class MatrixMenuItem(val title: String, val detail: String = "",
    val role: MatrixControlRole = MatrixControlRole.Option, val checked: Boolean? = null,
    val selected: Boolean? = null, val id: String = title, val isStatus: Boolean = false)

internal fun List<Pair<String, String>>.menuItems() = map { MatrixMenuItem(it.first, it.second) }

internal fun Modifier.matrixChoice(checked: Boolean? = null, chosen: Boolean? = null) = semantics {
    if (checked != null) {
        role = Role.Checkbox
        toggleableState = ToggleableState(checked)
        stateDescription = if (checked) "On" else "Off"
    } else if (chosen != null) {
        role = Role.RadioButton
        selected = chosen
        stateDescription = if (chosen) "Selected" else "Not selected"
    }
}

@Composable
internal fun MatrixMenu(title: String, entries: List<MatrixMenuItem>, back: () -> Unit,
    description: String = "", select: (Int) -> Unit) {
    val first = remember { FocusRequester() }
    // Status rows are plain text, never focusable: initial focus starts on the first clickable row.
    var focusedId by rememberSaveable(title) { mutableStateOf(entries.firstOrNull { !it.isStatus }?.id) }
    val ids = remember(entries) { entries.map { it.id } }
    val restoreId = remember(title, ids) {
        focusedId?.takeIf { id -> entries.any { it.id == id && !it.isStatus } } ?: entries.firstOrNull { !it.isStatus }?.id
    }
    val list = rememberLazyListState()
    val inspection = LocalMatrixUiInspection.current
    val openedAt = remember(title) { android.os.SystemClock.uptimeMillis() }
    var sawMenuDown by remember(title) { mutableStateOf(false) }
    SideEffect {
        MatrixSettingsDiag.log("menu-render") { "title=$title selected=${entries.mapIndexedNotNull { i, e -> i.takeIf { e.selected == true } }} checked=${entries.mapIndexedNotNull { i, e -> i.takeIf { e.checked == true } }}" } // DIAG-BUG2
        val index = entries.indexOfFirst { it.id == focusedId }
        if (index >= 0 && inspection?.focusedOption == index) {
            val entry = entries[index]
            inspection.focusedState = entry.checked?.let { if (it) "on" else "off" }
                ?: entry.selected?.let { if (it) "selected" else "unselected" } ?: "action"
        }
    }
    Column {
        MatrixPageHeader(title, back, if (entries.isEmpty()) first else null)
        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        MatrixScrollViewport(Modifier.weight(1f)) {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize()
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    val isSelect = native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                        native.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                        native.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A
                    if (isSelect) {
                        if (native.action == android.view.KeyEvent.ACTION_DOWN) {
                            if (native.repeatCount == 0) sawMenuDown = true
                            false
                        } else if (native.action == android.view.KeyEvent.ACTION_UP) {
                            if (!sawMenuDown) true else false
                        } else false
                    } else false
                }
                .matrixScrollEdges({ list.canScrollBackward }, { list.canScrollForward }),
                verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(MatrixLayout.focusInset)) {
                itemsIndexed(entries, key = { _, entry -> entry.id }, contentType = { _, entry -> entry.role to entry.isStatus }) { index, entry ->
                    if (entry.isStatus) {
                        // Information only: plain text, not a card and not a tile. TV
                        // Material 3 has no non-clickable card, so any container reads
                        // as something to tap. A title with a fully wrapped subheader,
                        // like the page header, keeps the facts readable and clearly
                        // non-actionable. No leading icon: text rows share one leading
                        // edge, so the title lines up with the action rows.
                        Column(
                            Modifier.fillMaxWidth().widthIn(max = 600.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .semantics(mergeDescendants = true) { },
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(entry.title, modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleSmall)
                            if (entry.detail.isNotBlank()) {
                                Text(entry.detail, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        return@itemsIndexed
                    }
                    val compact = entry.role == MatrixControlRole.Commit
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (compact) Alignment.CenterEnd else Alignment.CenterStart) {
                        MatrixButton(onClick = {
                            if (sawMenuDown || android.os.SystemClock.uptimeMillis() - openedAt >= 600L) select(index)
                        }, role = entry.role,
                            modifier = (if (compact) Modifier else Modifier.fillMaxWidth())
                                .then(if (entry.id == restoreId) Modifier.focusRequester(first) else Modifier)
                                .matrixChoice(entry.checked, entry.selected)
                                .onFocusChanged { if (it.isFocused) {
                                    focusedId = entry.id
                                    inspection?.focusedOption = index
                                    inspection?.focusedState = entry.checked?.let { value -> if (value) "on" else "off" }
                                        ?: entry.selected?.let { value -> if (value) "selected" else "unselected" } ?: "action"
                                } else if (inspection?.focusedOption == index) {
                                    inspection.focusedOption = -1
                                    inspection.focusedState = "none"
                                } }) {
                            if (!compact || entry.detail.isNotBlank()) MatrixRowText(entry.title, entry.detail, Modifier.weight(1f))
                            else Text(entry.title)
                            val state = entry.checked ?: entry.selected
                            if (state != null) {
                                Spacer(Modifier.width(12.dp))
                                Text(if (state) "✓" else "○", style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.clearAndSetSemantics { })
                            }
                        }
                    }
                }
            }
        }
    }
    val focusRemoved = focusedId != null && entries.none { it.id == focusedId && !it.isStatus }
    LaunchedEffect(title, focusRemoved) {
        val index = entries.indexOfFirst { it.id == restoreId }.coerceAtLeast(0)
        if (entries.isNotEmpty()) list.scrollToItem(index)
        withFrameNanos { }; first.requestFocus()
    }
}
