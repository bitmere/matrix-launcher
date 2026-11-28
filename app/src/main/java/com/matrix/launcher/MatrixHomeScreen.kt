@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.matrix.launcher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive

@Immutable
internal data class OrderEntry(val id: String, val title: String, val app: LaunchApp? = null, val media: MediaCard? = null, val icon: Int? = null)
@Immutable
internal data class OrderPage(val title: String, val preference: String, val entries: List<OrderEntry>, val selected: String = "", val visibility: Boolean = false)

@Composable
internal fun MatrixHomeApp(snapshot: HomeSnapshot, loading: Boolean, clock: LocalDateTime, repository: ComposeHomeRepository,
                          refresh: (Boolean) -> Unit, allowListings: () -> Unit, open: (Intent) -> Unit, notice: (String) -> Unit,
                          request: HomeNavigationRequest, wallpaper: String?, network: NetworkState, wallpaperChanged: () -> Unit, listingsAllowed: Boolean,
                          pageChanged: (String) -> Unit, reloadStatus: String = "", wallpaperGeneration: Long = 0L) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) mediaJob?.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); mediaJob?.cancel() }
    }
    val history = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf<String>() }
    val pageState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val page = history.lastOrNull() ?: "home"
    LaunchedEffect(page) { pageChanged(page) }
    var orderPage by remember { mutableStateOf<OrderPage?>(null) }
    var destination by rememberSaveable { mutableStateOf("") }
    var listPage by rememberSaveable { mutableIntStateOf(1) }
    var listReset by remember { mutableIntStateOf(0) }
    var listAtFirst by remember { mutableStateOf(true) }
    var listData by remember { mutableStateOf<Pair<MediaRow, Boolean>?>(null) }
    var listError by remember { mutableStateOf("") }
    var inputs by remember { mutableStateOf<List<InputTarget>>(emptyList()) }
    var inputError by remember { mutableStateOf("") }
    var managedApp by remember { mutableStateOf<LaunchApp?>(null) }
    // Recomposition on settings changes is owned by the repository now: settingsState
    // exposes a StateFlow snapshot, so no SharedPreferences listener is needed here.
    val settingsSnapshot by repository.settingsState.snapshot.collectAsState()
    settingsSnapshot.revision
    LaunchedEffect(settingsSnapshot.revision) {
        MatrixSettingsDiag.log("parent-revision") { "rev=${settingsSnapshot.revision}" } // DIAG-BUG2
    }
    LaunchedEffect(page, orderPage, managedApp) {
        // A killed process can restore navigation, but not an in-memory movement/app context.
        // Return to its parent rather than presenting an empty panel.
        if ((page == "order" && orderPage == null) || (page == "app-options" && managedApp == null)) {
            if (history.isNotEmpty()) history.removeAt(history.lastIndex)
        }
    }
    fun goHome() { mediaJob?.cancel(); history.toList().distinct().forEach(pageState::removeState); history.clear() }
    LaunchedEffect(request) { if (request.sequence > 0) { goHome(); if (request.page != "home") history.add(request.page) } }
    fun push(next: String) { mediaJob?.cancel(); history.add(next) }
    fun accountConnected() {
        val next = connectedAccountHistory(history.toList())
        history.clear(); history.addAll(next)
        notice("SIMKL connected. Choose the lists and rows you want on Home.")
        refresh(false)
    }
    fun back() {
        mediaJob?.cancel()
        if (matrixBackResetsList(page, listPage, listAtFirst)) { listPage = 1; listAtFirst = true; listReset++ }
        else if (history.isNotEmpty()) pageState.removeState(history.removeAt(history.lastIndex))
    }
    // One scope for the data-driven home settings pages (see MatrixSettingsPages.kt).
    // It only carries values and callbacks that already exist in this composable.
    val settingsScope = MatrixSettingsPageScope(
        repository = repository, snapshot = snapshot, loading = loading, reloadStatus = reloadStatus,
        listingsAllowed = listingsAllowed, allowListings = allowListings,
        push = ::push, back = ::back, refresh = refresh, open = open,
        openOrderPage = { orderPage = it }, context = context
    )
    fun toggleListPin(id: String) {
        val current = repository.prefs.getStringSet(MatrixPrefs.SIMKL_PINS, emptySet()).orEmpty().toMutableSet()
        val added = current.add(id)
        if (!added) current.remove(id)
        repository.saveSimklRows(MatrixPrefs.SIMKL_PINS, current, "simkl-list:")
        notice(if (added) "List pinned to Home." else "List removed from Home.")
        refresh(false)
    }
    fun favoritesOrder(ids: List<String>) {
        orderPage = OrderPage("Favorites · order", MatrixPrefs.FAVORITES, ids.mapNotNull { id ->
            snapshot.apps.find { it.id == id }?.let { OrderEntry(id, it.title, app = it) }
        }); push("order")
    }
    fun mediaOrder(row: MediaRow, card: MediaCard) {
        orderPage = OrderPage(row.title + " · card order", MatrixPrefs.itemOrder(row.id),
            row.cards.filterNot { it.link.startsWith("simkl-") }.map { OrderEntry(it.link, it.title, media = it) }, card.link)
        push("order")
    }
    fun activate(card: MediaCard) {
        if (card.link.startsWith("simkl-list:") || card.link.startsWith("simkl-library:")) {
            destination = card.link; listPage = 1; listAtFirst = true; listReset++; push("list")
        } else {
            mediaJob?.cancel()
            mediaJob = scope.launch {
                try {
                    val intent = repository.mediaIntent(card)
                    coroutineContext.ensureActive()
                    if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) open(intent)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (failure: Exception) { notice(failure.message ?: "Cannot open this title.") }
            }
        }
    }
    LaunchedEffect(page, destination, listPage) {
        if (page == "list") { listData = null; listError = ""; try { listData = repository.list(destination, listPage) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { listError = "List unavailable. Check the connected account in Settings → Integrations → SIMKL." } }
        if (page == "inputs") {
            inputs = emptyList(); inputError = ""
            try { inputs = repository.inputs() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { inputError = "The TV input service is unavailable. Use the TV's own input menu." }
        }
    }
    BackHandler(enabled = page != "home" && page != "order") { back() }

    val reducedMotion = rememberMatrixReducedMotion()
    val glassSource = rememberMatrixGlassSource(repository, wallpaper, wallpaperGeneration, reducedMotion)
    val inheritedCapabilities = LocalMatrixCapabilities.current
    val glassCapabilities = inheritedCapabilities.copy(
        material = if (glassSource.allocationFailed) MatrixMaterialQuality.CHEAP else inheritedCapabilities.material,
        motion = if (reducedMotion) MatrixMotionPolicy.REDUCED else inheritedCapabilities.motion)
    CompositionLocalProvider(LocalMatrixGlassSource provides glassSource, LocalMatrixCapabilities provides glassCapabilities) {
    CompositionLocalProvider(LocalMatrixSurfaceVisible provides (page == "home")) {
    MatrixHome(snapshot, loading, clock, repository, page == "home", if (glassCapabilities.wallpaper) wallpaper else null, network, request.focusReset,
        settings = { push("settings") }, apps = { push("apps") }, refresh = { refresh(true) },
        inputs = { push("inputs") }, wifi = { open(SystemSettings.intent(context, Settings.ACTION_WIFI_SETTINGS)) }, search = { push("search") },
        appClick = { repository.appIntent(it.id)?.let(open) }, favoritesEdit = { push("favorites") },
        mediaClick = ::activate, mediaHold = ::mediaOrder,
        appHold = { managedApp = it; push("app-options") })
    }

    if (page != "home") {
        val wide = page in listOf("apps", "favorites", "sources", "list", "search", "wallpaper-gallery") || page.startsWith("wallpaper-device:")
        Dialog(onDismissRequest = { if (page != "order") back() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = page != "order")) {
            MatrixDialogBlur()
            var sawFreshDown by remember(page) { mutableStateOf(false) }
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val isSelect = native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                    native.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                    native.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                    native.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A
                if (isSelect) {
                    when (native.action) {
                        android.view.KeyEvent.ACTION_DOWN -> {
                            if (native.repeatCount == 0) {
                                sawFreshDown = true
                                false
                            } else if (!sawFreshDown) {
                                true // Swallow residual repeat down events from prior long-press
                            } else false
                        }
                        android.view.KeyEvent.ACTION_UP -> {
                            if (!sawFreshDown) {
                                true // Swallow orphan up event from prior long-press
                            } else false
                        }
                        else -> false
                    }
                } else false
            }, contentAlignment = Alignment.CenterEnd) {
                val windowWidth = maxWidth
                MatrixPanel(wide) {
                    CompositionLocalProvider(LocalMatrixPagePath provides matrixPagePath(history.dropLast(1))) {
                    pageState.SaveableStateProvider(page) {
                    key(page) { when {
                        page.startsWith("simkl") -> MatrixAccountScreen(page, repository, ::push, ::back, refresh, ::accountConnected) { destination = it; listPage = 1; listAtFirst = true; listReset++; push("list") }
                        page == "wallpapers" || page.startsWith("wallpaper-") -> MatrixWallpaperScreen(page, repository, windowWidth, ::push, ::back, wallpaperChanged, notice)
                        page.startsWith("home-") -> MatrixHomeControlScreen(page, repository, ::push, ::back, open, notice)
                        page == "search" -> MatrixSearchScreen(repository, windowWidth, ::back, { push("integrations") }, ::activate)
                        page == "integrations" || page == "media-app" || page == "catalog" || page.startsWith("tmdb") || page.startsWith("ai") -> MatrixIntegrationsScreen(page, repository, ::push, ::back)
                        else -> when (page) {
                        // Home settings pages are data-driven: see matrixHomeSettingsPages
                        // in MatrixSettingsPages.kt. Only the dispatch lives here.
                        "settings", "appearance", "idle-clock", "channels", "limits", "size", "about", "theme", "backup" -> {
                            val def = matrixHomeSettingsPage(page)
                            if (def != null) MatrixSettingsPage(def, settingsScope)
                        }
                        "apps", "favorites", "sources" -> MatrixAppGrid(page, snapshot, repository, refresh, ::back, ::favoritesOrder,
                            { managedApp = it; push("app-options") }) { app -> repository.appIntent(app.id)?.let(open) }
                        "app-options" -> managedApp?.let { app -> SettingsPage(app.title, "Changes affect only Matrix. App removal always uses the system confirmation.", listOf(
                            SettingAction(if (app.id in snapshot.favorites) "Remove favorite" else "Add favorite") {
                                repository.saveFavorites(if (app.id in snapshot.favorites) snapshot.favorites - app.id else snapshot.favorites + app.id); refresh(false); back()
                            },
                            SettingAction("Open", role = MatrixControlRole.Action) { repository.appIntent(app.id)?.let(open) },
                            SettingAction("Reorder favorites") { favoritesOrder(snapshot.favorites) },
                            SettingAction(if (AppManagement.canUninstall(context, app.id)) "Uninstall app" else "App info", role = if (AppManagement.canUninstall(context, app.id)) MatrixControlRole.Destructive else MatrixControlRole.Option) { AppManagement.intent(context, app.id)?.let(open) }
                        ), ::back) }
                        "aot-control" -> {
                            var aotStatus by remember { mutableStateOf("Checking DEX status…") }
                            var compiling by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                val s = AotOptimizationManager.checkStatus(context, "com.stremio.one")
                                val n = AotOptimizationManager.checkStatus(context, "com.nuvio.tv")
                                aotStatus = "Stremio: $s · Nuvio: $n"
                            }
                            SettingsPage(stringResource(R.string.settings_aot_title),
                                stringResource(R.string.settings_aot_description),
                                listOf(
                                    SettingAction(if (compiling) stringResource(R.string.settings_aot_compiling_title) else stringResource(R.string.settings_aot_compile_title),
                                        aotStatus,
                                        role = MatrixControlRole.Action) {
                                        if (!compiling) {
                                            compiling = true
                                            scope.launch {
                                                val res = AotOptimizationManager.compileAllTracked(context)
                                                aotStatus = res.entries.joinToString(" · ") { "${it.key}: ${it.value}" }
                                                compiling = false
                                            }
                                        }
                                    },
                                    // Informational status rows, not actions: rendered as
                                    // plain text by MatrixMenu (SettingAction.isStatus), so
                                    // they are never focusable or clickable and take no handler.
                                    SettingAction(stringResource(R.string.settings_aot_auto_recompile_title),
                                        stringResource(R.string.settings_aot_auto_recompile_detail), isStatus = true),
                                    SettingAction(stringResource(R.string.settings_aot_local_port_title),
                                        stringResource(R.string.settings_aot_local_port_detail), isStatus = true)
                                ), ::back)
                        }
                        "data-credits" -> MatrixDataCredits(::back)
                        "licenses" -> MatrixLicenseScreen(::back)
                        "inputs" -> MatrixMenu("Inputs", inputs.map { MatrixMenuItem(it.title, id = it.id) }, ::back, inputError) { open(inputs[it].intent) }
                        "order" -> orderPage?.let { MatrixOrder(it, repository, refresh, ::back) }
                        "list" -> MatrixList(listData, listError, listPage, windowWidth, MatrixSafeMode.effectiveColumns(repository.prefs, repository.safeMode), repository,
                            ::back, ::goHome, { listPage++; listAtFirst = true }, listReset, { listAtFirst = it }, destination, ::toggleListPin, ::activate)
                    }
                    } }
                    }
                    }
                }
            }
        }
    }
    }
}

