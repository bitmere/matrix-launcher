package com.matrix.launcher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileDescriptor
import java.io.PrintWriter
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

/** Lifecycle-owned Home data and navigation. Accessibility takeover remains an explicit setting. */
class ComposeHomeActivity : ComponentActivity() {
    private lateinit var repository: ComposeHomeRepository
    private var nowPlayingJob: Job? = null
    private var snapshot by mutableStateOf(HomeSnapshot())
    private var loading by mutableStateOf(false)
    private var reloadStatus by mutableStateOf("")
    private var noticeToast: Toast? = null
    private var listingsAllowed by mutableStateOf(false)
    private var clock by mutableStateOf(LocalDateTime.now())
    private var refreshJob: Job? = null
    private var observing = false
    private val remoteConfig = RemoteConfig.Loader()
    private var request by mutableStateOf(HomeNavigationRequest())
    private var wallpaper by mutableStateOf<String?>(null)
    private var network by mutableStateOf(NetworkState())
    private var wallpaperJob: Job? = null
    private val inspection = MatrixUiInspection()
    private val keyLatency = MatrixKeyLatency()
    private var safeMode = false
    private var capabilities = MatrixCapabilities.FULL
    private var homeActive = true
    private var stoppedSinceResume = true
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleClock = Runnable { if (homeActive && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) open(Intent(this, ClockActivity::class.java)) }
    private fun scheduleIdle() {
        idleHandler.removeCallbacks(idleClock)
        val minutes = repository.prefs.getInt("idle_clock_minutes", 0).coerceIn(0, 60)
        if (homeActive && minutes > 0) idleHandler.postDelayed(idleClock, minutes * 60_000L)
    }
    override fun onUserInteraction() { super.onUserInteraction(); scheduleIdle() }
    // dispatchKeyEvent is restricted to the androidx library group. The
    // override is legitimate — it only records key timing for the bench
    // recorder — and it must call super to preserve normal dispatch.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyLatency.enabled && event.action == KeyEvent.ACTION_DOWN) {
            keyLatency.recordKeyDown(event.eventTime)
            Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
                keyLatency.recordFrame(frameTimeNanos)
            }
        }
        return super.dispatchKeyEvent(event)
    }
    private val packages = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            repository.invalidateApps()
            if (homeActive) refresh()
        }
    }
    private val listings = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        listingsAllowed = it; observeListings(); refresh()
    }
    // Storage Access Framework launchers for backup export/import and theme
    // import. The pending callback is set by the settings page that asked;
    // the result is delivered once and then cleared.
    private var documentResult: ((android.net.Uri?) -> Unit)? = null
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        documentResult?.invoke(uri); documentResult = null
    }
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(MatrixBackup.MIME_TYPE)) { uri ->
        documentResult?.invoke(uri); documentResult = null
    }
    private fun requestListings() {
        if (listingsAllowed) return
        val requested = repository.prefs.getBoolean("listings_permission_requested", false)
        if (requested && !shouldShowRequestPermissionRationale("android.permission.READ_TV_LISTINGS")) {
            notice("Enable TV listings access in this app's Permissions settings.")
            open(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        } else {
            repository.prefs.edit { putBoolean("listings_permission_requested", true) }
            listings.launch("android.permission.READ_TV_LISTINGS")
        }
    }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (!observing || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            refreshJob?.cancel(); refreshJob = lifecycleScope.launch { delay(1500); read(false) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); dispatchRequest(intent)
    }
    private fun dispatchRequest(intent: Intent) {
        val page = when (HomeRequest.page(intent.action)) {
            HomeRequest.Page.INPUTS -> "inputs"
            HomeRequest.Page.APPS -> "apps"
            HomeRequest.Page.SETTINGS -> "settings"
            else -> "home"
        }
        val resetFocus = page == "home" && homeActive && !stoppedSinceResume
        request = HomeNavigationRequest(page, request.sequence + 1, request.focusReset + if (resetFocus) 1 else 0)
        intent.action = null
        intent.removeCategory(Intent.CATEGORY_HOME)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Key-latency recorder (bench task 3): off unless the launching intent
        // carries the explicit debug flag, e.g. `am start ... --ez
        // matrix_key_latency true`. Inert when off: no timestamps, no callbacks.
        keyLatency.enabled = intent.getBooleanExtra("matrix_key_latency", false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        repository = ComposeHomeRepository(applicationContext)
        remoteConfig.load(this) { remote ->
            val inputComponent = android.content.ComponentName(this, InputsRequestActivity::class.java)
            val inputState = packageManager.getComponentEnabledSetting(inputComponent)
            val desiredState = if (remote.handleViewInputs) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            if (inputState != desiredState) packageManager.setComponentEnabledSetting(inputComponent, desiredState, PackageManager.DONT_KILL_APP)
            if (remote.restoreKeyServices) ServiceRepair.restore(this)
        }
        HomeRecovery.repair(this)
        if (!repository.prefs.contains("matrix_home_redirect")) {
            repository.prefs.edit {
                putBoolean("matrix_home_redirect", true)
                putBoolean("matrix_home_key_filter", true)
            }
        }
        if (HomeRequest.resetOnCreate(savedInstanceState != null, intent.action, intent.hasCategory(Intent.CATEGORY_HOME))) dispatchRequest(intent)
        androidx.core.content.ContextCompat.registerReceiver(this, packages,
            android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
            }, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        // Safe mode is computed here, before setContent: the crash counter is
        // incremented with commit() so a crash before composition still counts.
        // `adb shell am start -n <pkg>/com.gathin.tvhome.ComposeHomeActivity
        // --ez safe_mode true` enters it for one launch.
        safeMode = MatrixSafeMode.onStartup(repository.prefs, intent.getBooleanExtra(MatrixSafeMode.INTENT_EXTRA, false))
        repository.safeMode = safeMode
        intent.removeExtra(MatrixSafeMode.INTENT_EXTRA)
        // One capability decision for old and new TVs: safe mode or a low-RAM
        // device gets the cheap variant (no glow sweep, no wallpaper).
        capabilities = MatrixCapabilities.decide(safeMode, getSystemService(ActivityManager::class.java)?.isLowRamDevice == true)
        MatrixTheme.apply(ThemeState.defaults())
        lifecycleScope.launch {
            val theme = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (safeMode) ThemeState.defaults() else MatrixTheme.load(filesDir)
            }
            MatrixTheme.apply(theme)
        }
        // Wire the SAF launchers for the settings pages (backup + theme import).
        MatrixSaf.owner = this
        MatrixSaf.cancelPending = { documentResult = null }
        MatrixSaf.openDocument = { mimeTypes, onResult -> documentResult = onResult; openDocument.launch(mimeTypes) }
        MatrixSaf.createDocument = { fileName, _, onResult -> documentResult = onResult; createDocument.launch(fileName) }
        setContent {
            // A clean start reached composition: reset the crash counter.
            androidx.compose.runtime.SideEffect { MatrixSafeMode.onFirstComposition(repository.prefs) }
            CompositionLocalProvider(LocalMatrixUiInspection provides inspection, LocalMatrixCapabilities provides capabilities) { MatrixTvTheme {
            MatrixHomeApp(snapshot, loading, clock, repository, ::refresh,
                ::requestListings, ::open, ::notice, request, wallpaper, network, ::wallpaperChanged, listingsAllowed,
                { inspection.page = it; homeActive = it == "home"; scheduleIdle() }, reloadStatus)
        } } }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { networkStates(applicationContext).collect { network = it } }
            launch { while (isActive) {
                // Cheap variant: no wallpaper (plain background).
                if (capabilities.wallpaper) {
                    repository.wallpapers.refresh(false)
                    wallpaper = repository.wallpapers.current()
                }
                delay(60_000)
            } }
            while (isActive) { clock = LocalDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L) }
        } }
    }
    override fun onStart() {
        super.onStart(); listingsAllowed = checkSelfPermission("android.permission.READ_TV_LISTINGS") == PackageManager.PERMISSION_GRANTED
        HomeRecovery.verifyWhenVisible(this); refresh(); scheduleIdle()
        observeListings()
        nowPlayingJob = lifecycleScope.launch {
            combine(NowPlayingMedia.state, repository.settingsState.snapshot) { item, _ -> item }
                .collect { item -> snapshot = repository.withNowPlaying(snapshot, item) }
        }
    }
    private fun observeListings() {
        if (!observing && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && checkSelfPermission("android.permission.READ_TV_LISTINGS") == PackageManager.PERMISSION_GRANTED) {
            try {
            for (table in listOf("channel", "watch_next_program", "preview_program")) {
                contentResolver.registerContentObserver("content://android.media.tv/$table".toUri(), true, observer)
            }; observing = true
            } catch (denied: SecurityException) { contentResolver.unregisterContentObserver(observer) }
        }
    }
    override fun onResume() {
        super.onResume(); stoppedSinceResume = false
    }
    override fun onStop() {
        nowPlayingJob?.cancel(); nowPlayingJob = null
        stoppedSinceResume = true
        idleHandler.removeCallbacks(idleClock)
        if (observing) { contentResolver.unregisterContentObserver(observer); observing = false }
        refreshJob?.cancel(); super.onStop()
    }
    override fun onDestroy() {
        MatrixSaf.release(this)
        documentResult = null
        noticeToast?.cancel(); remoteConfig.cancel(); unregisterReceiver(packages); repository.clear(); super.onDestroy()
    }
    private fun wallpaperChanged() {
        if (!capabilities.wallpaper) return
        wallpaperJob?.cancel(); wallpaperJob = lifecycleScope.launch { wallpaper = repository.wallpapers.current() }
    }
    private fun refresh(force: Boolean = false) {
        if (force && loading) return
        refreshJob?.cancel(); refreshJob = lifecycleScope.launch { read(force) }
    }
    private suspend fun read(force: Boolean) {
        loading = true
        if (force) { reloadStatus = "Reloading Home…"; notice(reloadStatus) }
        try {
            snapshot = repository.withNowPlaying(repository.load(force))
            if (force) {
                reloadStatus = "Home reload finished. " + snapshot.status
                notice(reloadStatus)
            }
        }
        catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (force) reloadStatus = "Reload interrupted. Try again."
            throw cancelled
        }
        catch (error: Exception) {
            val message = "Home could not refresh. App shortcuts remain available."
            if (force) reloadStatus = message
            notice(message)
        }
        finally { if (kotlinx.coroutines.currentCoroutineContext().isActive) loading = false }
    }
    override fun dump(prefix: String, fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        if (args?.contains("--matrix-ui") != true) { super.dump(prefix, fd, writer, args); return }
        writer.println("${prefix}MatrixUi page=${inspection.page} focusedOption=${inspection.focusedOption} state=${inspection.focusedState}")
        writer.println("${prefix}homeRow=${inspection.homeRow} homeCard=${inspection.homeCard} listingsAllowed=$listingsAllowed")
        writer.println("${prefix}ReloadStatus=$reloadStatus")
        writer.println("${prefix}KeyLatency ${keyLatency.summary()}")
        writer.println("${prefix}safeMode=$safeMode crashes=${repository.prefs.getInt(MatrixPrefs.SAFE_MODE_CRASHES, 0)}")
        writer.println("${prefix}theme=${if (MatrixLayout.theme.fromFile) "file" else "default"}${MatrixLayout.theme.ignoredReason?.let { " ignored: $it" }.orEmpty()}")
        writer.println("${prefix}loading=$loading apps=${snapshot.apps.size} rows=${snapshot.rows.size} homeConnected=${HomeKeyService.isConnected()}")
        writer.println("${prefix}recovery=${HomeRecovery.enabled(this)} lifecycle=${lifecycle.currentState}")
        writer.println("${prefix}HomeViewport ${inspection.verticalSummary()}")
        writer.println("${prefix}SearchUi available=${repository.search.providers().joinToString(",") { it.name }.ifEmpty { "none" }} selected=${inspection.searchProvider} results=${inspection.searchResults} busy=${inspection.searchBusy}")
        writer.println("${prefix}Simkl connected=${repository.accounts.connected} enabled=${repository.prefs.getBoolean("simkl_home_enabled", false)} pins=${repository.prefs.getStringSet("simkl_pins", emptySet()).orEmpty().size} visibleCustom=${snapshot.rows.count { it.id.startsWith("simkl-list:") }} availableCustom=${snapshot.allRows.count { it.id.startsWith("simkl-list:") }}")
        writer.println("${prefix}MediaTarget preference=${repository.media.preferred}")
        val nuvio = snapshot.allRows.filter { it.id.startsWith("com.nuvio.tv:") }.flatMap { it.cards }
        nuvio.mapNotNull { runCatching { Intent.parseUri(it.link, Intent.URI_INTENT_SCHEME) }.getOrNull() }
            .groupingBy { "flags=${Integer.toHexString(it.flags)} uri=${it.data != null} content=${it.hasExtra("contentId")} stream=${it.getStringExtra("launchMode") == "stream"}" }
            .eachCount().forEach { (route, count) -> writer.println("${prefix}NuvioRoute $route count=$count") }
        inspection.icons.toSortedMap().forEach { (label, bounds) -> writer.println("${prefix}IconLayout $label ${bounds.summary()}") }
        inspection.rows.toSortedMap().forEach { (row, geometry) -> writer.println("${prefix}RowLayout $row ${geometry.summary()}") }
    }
    private fun open(intent: Intent) {
        try { startActivity(intent) } catch (error: android.content.ActivityNotFoundException) { notice("This action is unavailable on this TV.") }
        catch (error: SecurityException) { notice("This TV did not allow that action.") }
    }
    private fun notice(message: String) {
        noticeToast?.cancel()
        noticeToast = Toast.makeText(this, message, Toast.LENGTH_LONG).also { it.show() }
    }
}

/**
 * Debug-flagged key→frame latency recorder (bench task 3).
 *
 * Off by default and inert when off: [recordKeyDown] and [recordFrame] return
 * immediately and no Choreographer callback is ever posted (the activity only
 * posts one when [enabled] is true). Enabled only by the explicit
 * `matrix_key_latency` launch extra, so release builds measure nothing
 * unless the bench asks for it. Samples are capped (256) and hold no
 * identifiers — just key-down to next-presented-frame durations in ms.
 * Exposed through the activity's [ComposeHomeActivity.dump] override so
 * `tools/bench.py` can read it with `dumpsys activity <pkg> --matrix-ui`.
 */
internal class MatrixKeyLatency {
    var enabled = false
    private val pending = ArrayDeque<Long>()
    private val samples = ArrayDeque<Long>()

    fun recordKeyDown(eventTimeMillis: Long) {
        if (!enabled) return
        pending.addLast(eventTimeMillis)
        if (pending.size > 8) pending.removeFirst()
    }

    fun recordFrame(frameTimeNanos: Long) {
        if (!enabled) return
        val keyAt = pending.removeFirstOrNull() ?: return
        val latency = frameTimeNanos / 1_000_000 - keyAt
        if (latency in 0..10_000) {
            samples.addLast(latency)
            if (samples.size > 256) samples.removeFirst()
        }
    }

    fun summary(): String {
        if (samples.isEmpty()) return "not measured"
        val sorted = samples.sorted()
        fun percentile(p: Double): Long {
            val index = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
            return sorted[index]
        }
        return "p50=${percentile(.5)} p95=${percentile(.95)} n=${samples.size}"
    }
}
