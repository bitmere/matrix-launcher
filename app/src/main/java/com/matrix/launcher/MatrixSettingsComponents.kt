package com.matrix.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

internal data class SettingAction(val title: String, val detail: String = "", val role: MatrixControlRole = MatrixControlRole.Option, val checked: Boolean? = null, val selected: Boolean? = null, val id: String = title, val isStatus: Boolean = false, val run: () -> Unit = {})

/** One row shape, typography and focus treatment for every feature's settings. Status rows (SettingAction.isStatus) are information only: MatrixMenu renders them as plain text — a title with a fully wrapped subheader, no container — never focusable or clickable, so they need no run handler. */
@Composable
internal fun SettingsPage(title: String, description: String, actions: List<SettingAction>, back: () -> Unit) {
    MatrixMenu(title, actions.map { MatrixMenuItem(it.title, it.detail, it.role, it.checked, it.selected, it.id, it.isStatus) }, back, description) { if (!actions[it].isStatus) actions[it].run() }
}

internal data class FormField(val key: String, val label: String, val initial: String = "", val secret: Boolean = false)

/** Shared field for setup and inline search; keyboard Back stays inside the field. */
@Composable
internal fun MatrixTextEntry(label: String, value: String, changed: (String) -> Unit, focus: FocusRequester,
    modifier: Modifier = Modifier, secret: Boolean = false, imeAction: ImeAction = ImeAction.Done,
    showLabel: Boolean = true, compact: Boolean = false, placeholder: String = "",
    next: () -> Unit, submit: () -> Unit = next) {
    val textFocus = remember { FocusRequester() }
    val colors = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    fun finish(action: () -> Unit) { keyboard?.hide(); editing = false; action() }
    fun stopEditing() { finish { scope.launch { withFrameNanos { }; focus.requestFocus() } } }
    BackHandler(editing) { stopEditing() }
    Column(modifier) {
        if (showLabel) Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        Surface(onClick = { editing = true; textFocus.requestFocus(); keyboard?.show() },
            modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) MatrixControlStyle.headerButtonSize else MatrixControlStyle.fieldHeight).focusRequester(focus),
            shape = ClickableSurfaceDefaults.shape(MatrixControlStyle.shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = MatrixLayout.cardFocusScale),
            colors = ClickableSurfaceDefaults.colors(containerColor = MatrixControlStyle.fill(colors), contentColor = colors.onSurface,
                focusedContainerColor = MatrixControlStyle.fill(colors), focusedContentColor = colors.onSurface),
            border = ClickableSurfaceDefaults.border(border = Border(MatrixControlStyle.stroke(colors)), focusedBorder = Border(BorderStroke(1.5.dp, colors.onSurface)))) {
            BasicTextField(value = value, onValueChange = changed, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.onSurface),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onNext = { finish(next) }, onDone = { finish(submit) }, onSearch = { finish(submit) }),
                decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    inner()
                } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 16.dp).focusRequester(textFocus)
                    .semantics { contentDescription = label }.onFocusChanged { if (it.isFocused) editing = true }
                    .onPreviewKeyEvent { event ->
                        when (event.key) {
                            Key.Back, Key.Escape -> { if (event.type == KeyEventType.KeyUp) stopEditing(); true }
                            Key.DirectionDown -> { if (event.type == KeyEventType.KeyDown) finish(next); true }
                            else -> false
                        }
                    })
        }
    }
}

/** TV keyboard stays local to the field; Back closes editing before leaving the page. */
@Composable
internal fun SettingsForm(title: String, help: String, fields: List<FormField>, back: () -> Unit, save: (Map<String, String>) -> Unit) {
    val values = remember(title) { mutableStateMapOf<String, String>().apply { fields.forEach { put(it.key, it.initial) } } }
    val fieldKeys = remember(fields) { fields.map { it.key } }
    val fieldFocus = remember(title, fieldKeys) { fields.map { FocusRequester() } }
    val saveFocus = remember { FocusRequester() }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSecrets by remember(title) { mutableStateOf(false) }
    val hasSecrets = fields.any { it.secret }
    fun finishEditing(target: FocusRequester) {
        scope.launch {
            val index = if (target === saveFocus) fields.size else fieldFocus.indexOf(target).coerceAtLeast(0)
            list.scrollToItem(index); withFrameNanos { }; target.requestFocus()
        }
    }
    Column {
        MatrixPageHeader(title, back)
        Text(help, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyMedium)
        MatrixScrollViewport(Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize().matrixScrollEdges({ list.canScrollBackward }, { list.canScrollForward }), state = list, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(fields, key = { _, field -> field.key }) { index, field ->
                MatrixTextEntry(field.label, values[field.key].orEmpty(), { values[field.key] = it }, fieldFocus[index],
                    secret = field.secret && !showSecrets, imeAction = if (index == fields.lastIndex) ImeAction.Done else ImeAction.Next,
                    next = { finishEditing(fieldFocus.getOrNull(index + 1) ?: saveFocus) })
            }

            item(key = "actions") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.End)) {
                    if (hasSecrets) {
                        MatrixIconButton(icon = if (showSecrets) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                            label = if (showSecrets) "Hide key" else "Show key", onClick = { showSecrets = !showSecrets })
                    }
                    MatrixButton(onClick = back, role = MatrixControlRole.Secondary) { Text("Cancel") }
                    MatrixButton(onClick = { save(values.toMap()) }, modifier = Modifier.focusRequester(saveFocus), role = MatrixControlRole.Commit) { Text("Save") }
                }
            }
        }
        }
    }
    LaunchedEffect(title) { withFrameNanos { }; (fieldFocus.firstOrNull() ?: saveFocus).requestFocus() }
}

/** Loading/error states use the same card surfaces as the content that replaces them. */
@Composable
internal fun MatrixLoadState(message: String, loading: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (loading) repeat(3) {
            Box(Modifier.fillMaxWidth(if (it == 2) .65f else 1f).height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MatrixControlStyle.shape))
        }
    }
}
