package com.matrix.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MatrixAccountScreen(page: String, repository: ComposeHomeRepository, push: (String) -> Unit,
    back: () -> Unit, refresh: (Boolean) -> Unit, connected: () -> Unit, list: (String) -> Unit) {
    val account = repository.accounts
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    when (page) {
        "simkl" -> {
            revision // Re-read after a local disconnect without rebuilding Home's app catalogue.
            val actions = if (!account.connected) listOf(
                SettingAction("Connect with QR code", "Approve on your phone; no password or client secret") { push(if (account.clientId.isBlank()) "simkl-client" else "simkl-connect") },
                SettingAction("Public Client ID", "Your AUTH V2 TV/device application") { push("simkl-client") }
            ) else listOf(
                SettingAction("Browse & Pin custom lists", "View cards in each list and toggle them on Home") { push("simkl-browse") },
                SettingAction("Quick toggle lists on Home", "Check off up to four cached previews") { push("simkl-pins") },
                SettingAction("Add list by ID", "Enter any public SIMKL list ID to view cards and pin to Home") { push("simkl-add-id") },
                SettingAction("Library rows on Home", "Watching, planned and completed sections") { push("simkl-sections") },
                SettingAction("Use in Search", "Show SIMKL as a search provider", checked = repository.search.enabled(MatrixSearchProvider.SIMKL)) {
                    repository.search.enable(MatrixSearchProvider.SIMKL, !repository.search.enabled(MatrixSearchProvider.SIMKL)); revision++
                },
                SettingAction("Show SIMKL rows", "App channels remain independent", checked = repository.prefs.getBoolean("simkl_home_enabled", false)) {
                    repository.prefs.edit { putBoolean("simkl_home_enabled", !repository.prefs.getBoolean("simkl_home_enabled", false)) }; revision++; refresh(false)
                },
                SettingAction("API usage & sync", "Read local quota status without making an API request") { push("simkl-usage") },
                SettingAction("Test connection", "Verify SIMKL token and access") { push("simkl-test") },
                SettingAction("Disconnect", "Remove this launcher's tokens and cached lists") { push("simkl-disconnect") }
            )
            SettingsPage(if (account.connected) "SIMKL · ${account.name}" else "SIMKL",
                if (account.connected) "SIMKL connected. Choose which lists and library rows appear on Home."
                else "Optional. Channels from installed apps work without a SIMKL account.", actions, back)
        }
        "simkl-client" -> SettingsForm("SIMKL application", error.ifBlank { "Register an AUTH V2 ‘TV, devices & command line’ app at simkl.com/settings/developer. Enter its public Client ID to enable QR sign-in." },
            listOf(FormField("id", "Public Client ID", account.clientId)), back) {
            if (it["id"].isNullOrBlank()) error = "Enter the public Client ID."
            else if (!busy) {
                val clientId = it.getValue("id").trim()
                busy = true
                scope.launch {
                    try { account.saveClientId(clientId); if (account.connected) connected() else push("simkl-connect") }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = "Could not save the public Client ID. Try again." }
                    finally { busy = false }
                }
            }
        }
        "simkl-connect" -> MatrixSimklConnect(account, back, connected)
        "simkl-usage" -> SettingsPage("SIMKL · sync", account.usage(), listOf(
            SettingAction("Connected Apps", "simkl.com/settings/connected-apps — review shared allowance on your phone") { push("simkl-usage-qr") }
        ), back)
        "simkl-usage-qr" -> MatrixQrPage("Connected Apps", "https://simkl.com/settings/connected-apps/", "Review your connected applications and account allowance on your phone.", back)
        "simkl-disconnect" -> SettingsPage("Disconnect SIMKL?", error.ifBlank { "Only this launcher's connection is removed. Nuvio and other apps keep their own accounts." },
            if (busy) emptyList() else listOf(SettingAction("Disconnect this launcher", role = MatrixControlRole.Destructive) {
                busy = true; scope.launch {
                    try { account.disconnect(); refresh(false); back() }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = "Disconnect could not complete. Try again." }
                    finally { busy = false }
                }
            }), back)
        "simkl-sections" -> MatrixSectionSelection(repository, back, refresh)
        "simkl-add-id" -> SettingsForm("Add SIMKL list", "Enter any public SIMKL list numeric ID (e.g. from simkl.com/list/12345). It will open and can be pinned to Home.",
            listOf(FormField("id", "SIMKL list ID")), back) {
            val id = it["id"]?.trim()?.filter { c -> c.isDigit() }.orEmpty()
            openSimklListId(id, back, list)
        }
        "simkl-pins", "simkl-browse" -> MatrixSimklLists(page == "simkl-pins", repository, back, refresh, list)
        "simkl-test" -> {
            val testState = produceState<Result<String>?>(initialValue = null, key1 = revision) {
                value = account.testConnection()
            }
            when (val res = testState.value) {
                null -> MatrixStatusPage("Test SIMKL", "Checking connection to SIMKL API…", true, back)
                else -> res.fold(
                    onSuccess = { msg ->
                        SettingsPage("SIMKL Connection", msg, listOf(
                            SettingAction("Done", "Return to SIMKL settings", role = MatrixControlRole.Commit) { back() }
                        ), back)
                    },
                    onFailure = { err ->
                        SettingsPage("SIMKL Connection Failed", err.message ?: "Unknown error", listOf(
                            SettingAction("Retry", "Test connection again", role = MatrixControlRole.Action) { revision++ },
                            SettingAction("Back", "Return to SIMKL settings", role = MatrixControlRole.Secondary) { back() }
                        ), back)
                    }
                )
            }
        }
    }
}

@Composable
private fun MatrixSimklConnect(account: MatrixAccountsRepository, back: () -> Unit, connected: () -> Unit) {
    var pin by remember { mutableStateOf<SimklClient.Pin?>(null) }
    var error by remember { mutableStateOf("") }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(account) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            try {
                // Reopening a completed flow must not issue a second device code.
                if (account.connected) { connected(); return@repeatOnLifecycle }
                val code = account.begin(); pin = code
                val job = currentCoroutineContext()
                while (isActive && System.currentTimeMillis() < code.expires) {
                    delay(code.interval * 1000L)
                    if (account.poll(code) { job.isActive }) { connected(); return@repeatOnLifecycle }
                }
                error = "This code expired. Go back and connect again."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = "Unable to connect. Check the public Client ID and your network." }
        }
    }
    if (pin != null && error.isBlank()) MatrixQrPage("Connect SIMKL", pin!!.url, "Code: ${pin!!.code}\nScan with your phone and approve read-only list access.", back)
    else MatrixStatusPage("Connect SIMKL", error.ifBlank { "Preparing approval code…" }, error.isBlank(), back)
}

@Composable
internal fun MatrixQrPage(title: String, url: String, help: String, back: () -> Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, url) { value = withContext(Dispatchers.Default) {
        val bits = com.google.zxing.qrcode.QRCodeWriter().encode(url, com.google.zxing.BarcodeFormat.QR_CODE, 360, 360)
        val pixels = IntArray(360 * 360) { i -> if (bits[i % 360, i / 360]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        android.graphics.Bitmap.createBitmap(pixels, 360, 360, android.graphics.Bitmap.Config.RGB_565)
    } }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MatrixPageHeader(title, back, focus)
        bitmap?.let { Image(it.asImageBitmap(), "Scan link with your phone", Modifier.size(200.dp)) }
        Text(help, style = MaterialTheme.typography.bodyLarge)
        Text(url, style = MaterialTheme.typography.bodyMedium)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun MatrixSectionSelection(repository: ComposeHomeRepository, back: () -> Unit, refresh: (Boolean) -> Unit) {
    var selected by remember { mutableStateOf(repository.prefs.getStringSet("simkl_sections", setOf("shows:watching", "movies:plantowatch", "shows:plantowatch", "anime:watching")).orEmpty().toSet()) }
    SettingsPage("SIMKL library rows", "Changes are applied only when you save.", HomeSections.LIBRARY.map { section ->
        SettingAction(section[1], checked = section[0] in selected, id = section[0]) {
            selected = selected.toMutableSet().apply { if (!add(section[0])) remove(section[0]) }
        }
    } + SettingAction("Save rows", role = MatrixControlRole.Commit) { repository.saveSimklRows("simkl_sections", selected, "simkl-library:"); refresh(false); back() }, back)
}

@Composable
private fun MatrixSimklLists(edit: Boolean, repository: ComposeHomeRepository, back: () -> Unit, refresh: (Boolean) -> Unit, open: (String) -> Unit) {
    var lists by remember { mutableStateOf<List<SimklListChoice>?>(null) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(repository.prefs.getStringSet("simkl_pins", emptySet()).orEmpty().toSet()) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = ""
        try { lists = repository.accounts.lists() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) { error = "Lists unavailable. Check account access or try again later." }
    }
    if (lists == null && error.isBlank()) MatrixStatusPage("SIMKL lists", "Loading cached lists…", true, back)
    else SettingsPage(if (edit) "Custom lists on Home" else "Your SIMKL lists", error.ifBlank { if (edit) "Choose up to four lists; save to apply." else "Select a list to browse." },
        lists.orEmpty().map { item ->
            val pinned = item.id in selected
            val detail = "${item.count} titles" + (if (pinned) " · Pinned to Home ✓" else if (!edit) " · Select to view cards" else "")
            SettingAction(item.title, detail, checked = if (edit) pinned else null, id = item.id) {
                if (!edit) open("simkl-list:${item.id}")
                else if (pinned || selected.size < 4) selected = selected.toMutableSet().apply { if (!add(item.id)) remove(item.id) }
            }
        } + if (error.isNotBlank()) listOf(SettingAction("Retry", role = MatrixControlRole.Action) { attempt++ }) else if (edit) listOf(SettingAction("Save lists", role = MatrixControlRole.Commit) {
            repository.saveSimklRows("simkl_pins", selected, "simkl-list:"); refresh(false); back()
        }) else emptyList(), back)
}

/** Replace the form before opening its list, so Back returns to SIMKL. */
internal fun openSimklListId(id: String, back: () -> Unit, open: (String) -> Unit) {
    if (id.isNotBlank() && id.all(Char::isDigit)) { back(); open("simkl-list:$id") }
}
