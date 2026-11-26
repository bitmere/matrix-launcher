package com.matrix.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Existing provider/cache/auth policies stay outside composition. Never reads the installed app's private data. */
internal class ComposeHomeRepository(private val context: Context) {
    val prefs = context.getSharedPreferences(MatrixPrefs.FILE, Context.MODE_PRIVATE)
    var safeMode: Boolean = MatrixSafeMode.isOn(prefs)
    val settingsState = MatrixSettingsState(prefs)
    private val simkl = SimklClient(context)
    private val artwork = HomeArtwork(context)
    /** Two locks, one order (D-03): Home work under homeGate, SIMKL client calls
     * under simklGate, then the client's own monitor — never the reverse. The
     * single gate used to serialize a slow SIMKL call against Home load itself. */
    private val homeGate = Mutex()
    private val simklGate = Mutex()
    val accounts = MatrixAccountsRepository(simkl, simklGate)
    val search = MatrixSearchRepository(prefs, simkl, simklGate, context).also { it.ai.safeMode = { safeMode } }
    val wallpapers = MatrixWallpaperRepository(context, prefs)
    private val colors = android.util.LruCache<String, ArtworkPalette>(128)
    private val appGeneration = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var appCatalogue: Pair<Int, List<LaunchApp>>? = null
    fun invalidateApps() { appGeneration.incrementAndGet(); appCatalogue = null; artwork.clear(); TvFeed.clearAppNameCache() }
    private fun installedApps(): List<LaunchApp> {
        val generation = appGeneration.get()
        appCatalogue?.takeIf { it.first == generation }?.let { return it.second }
        val pm = context.packageManager
        val intents = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        return intents.flatMap { category -> pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0) }
            .map { it.activityInfo.packageName }.distinct().filter { it != context.packageName }
            .map { LaunchApp(it, TvFeed.appName(context, it)) }.sortedBy { it.title.lowercase() }.also { appCatalogue = generation to it }
    }
    suspend fun searchApps(query: String): List<LaunchApp> = withContext(Dispatchers.IO) { filterApps(installedApps(), query) }
    suspend fun palette(uri: String): ArtworkPalette? = withContext(Dispatchers.IO) {
        if (uri.isBlank()) return@withContext null
        colors.get(uri)?.let { return@withContext it }
        val request = coil.request.ImageRequest.Builder(context).data(uri).size(32).allowHardware(false).build()
        val result = coil.Coil.imageLoader(context).execute(request)
        if (result is coil.request.SuccessResult) ArtworkColor.ambientColors(result.drawable).let { ArtworkPalette(it[0], it[1]) }.also { colors.put(uri, it) } else null
    }

    suspend fun load(force: Boolean = false): HomeSnapshot = withContext(Dispatchers.IO) { homeGate.withLock {
        val apps = installedApps()
        val safeMode = this@ComposeHomeRepository.safeMode
        if (!prefs.contains(MatrixPrefs.FAVORITES)) saveFavorites(apps.take(4).map { it.id })
        val favorites = runCatching { val array = JSONArray(prefs.getString(MatrixPrefs.FAVORITES, "[]"));
            List(array.length()) { array.getString(it) } }.getOrDefault(emptyList()).filter { id -> apps.any { it.id == id } }
        val result = TvFeed.read(context, prefs.getStringSet(MatrixPrefs.SOURCES, emptySet()).orEmpty())
        // Safe mode skips SIMKL sections/pins and custom lists: only app channels + favorites.
        if (!safeMode && simkl.connected() && prefs.getBoolean(MatrixPrefs.SIMKL_HOME_ENABLED, false)) {
            // SIMKL feed sections under the SIMKL lock (home → SIMKL order):
            // a slow SIMKL call stalls only this section, not Home load itself.
            runCatching { simklGate.withLock {
                if (force) simkl.refreshOnNextRead()
                HomeFeedSource.append(result, prefs, simkl)
            } }
                .onFailure { result.status += " · SIMKL: sync unavailable; cached app rows remain usable" }
        }
        HomeSnapshot(apps, favorites, HomeRows.visible(result, prefs).map(::row), HomeRows.orderedRows(result, prefs).map(::row), result.status)
    } }
    fun withNowPlaying(snapshot: HomeSnapshot, item: NowPlayingItem? = NowPlayingMedia.state.value): HomeSnapshot {
        val content = snapshot.rows.filterNot { it.id == MatrixPrefs.NOW_PLAYING_ROW }
        val shown = MatrixPrefs.NOW_PLAYING_ROW !in prefs.getStringSet(MatrixPrefs.HIDDEN_ROWS, emptySet()).orEmpty()
        val row = item?.takeIf { shown }?.let {
            MediaRow(MatrixPrefs.NOW_PLAYING_ROW, context.getString(R.string.now_playing_title), emptyList(), it)
        }
        return snapshot.copy(rows = listOfNotNull(row) + content)
    }

    private fun row(value: TvFeed.Row) = MediaRow(value.key, value.title, value.items.map {
        MediaCard(it.pkg + ":" + it.intent, it.title.orEmpty(), it.poster.orEmpty(), it.intent.orEmpty(),
            it.pkg.orEmpty(), it.portrait, it.position, it.duration, simklReference = it.simklReference.orEmpty())
    }.distinctBy { it.id })
    suspend fun banner(pkg: String): Pair<Bitmap, Int>? = withContext(Dispatchers.IO) {
        artwork.get(pkg)?.let { it.bitmap to it.accent }
    }
    fun cachedBanner(pkg: String?): Pair<Bitmap, Int>? = pkg?.let { artwork.peek(it)?.let { art -> art.bitmap to art.accent } }
    fun saveFavorites(ids: List<String>) { prefs.edit { putString(MatrixPrefs.FAVORITES, JSONArray(ids).toString()) } }
    fun saveSimklRows(key: String, ids: Set<String>, prefix: String) {
        val hidden = prefs.getStringSet(MatrixPrefs.HIDDEN_ROWS, emptySet()).orEmpty() - ids.map { prefix + it }.toSet()
        prefs.edit { putStringSet(key, ids); putStringSet(MatrixPrefs.HIDDEN_ROWS, hidden); putBoolean(MatrixPrefs.SIMKL_HOME_ENABLED, true) }
    }
    fun rowShown(id: String, shown: Boolean) {
        val hidden = prefs.getStringSet(MatrixPrefs.HIDDEN_ROWS, emptySet()).orEmpty().toMutableSet()
        if (shown) hidden.remove(id) else hidden.add(id)
        prefs.edit { putStringSet(MatrixPrefs.HIDDEN_ROWS, hidden) }
    }
    fun saveOrder(key: String, ids: List<String>) { prefs.edit { putString(key, JSONArray(ids).toString()) } }
    suspend fun list(destination: String, page: Int): Pair<MediaRow, Boolean> = withContext(Dispatchers.IO) { simklGate.withLock {
        if (destination.startsWith("simkl-library:")) {
            val parts = destination.removePrefix("simkl-library:").split(':')
            val full = simkl.library(parts[0], parts[1], page); row(full) to full.hasMore
        } else {
            val data = simkl.list(destination.removePrefix("simkl-list:"), page)
            val full = TvFeed.Row(data.optString("name", "SIMKL list"))
            full.items.addAll(SimklClient.mediaItems(data.optJSONArray("items"), data.optString("media_type"), 24))
            row(full) to (page < (data.optJSONObject("pagination")?.optInt("total_pages", 1) ?: 1))
        }
    } }
    suspend fun inputs(): List<InputTarget> = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(TvInputManager::class.java)
        val inputs = manager?.tvInputList.orEmpty().filter { it.isPassthroughInput && !it.isHidden(context) }
        val parents = inputs.mapNotNull { it.parentId }.toSet()
        val targets = inputs.filter { it.id !in parents }.map {
            InputTarget(it.id, (it.loadCustomLabel(context)?.takeIf { s -> s.isNotBlank() } ?: it.loadLabel(context)).toString(),
                Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelUriForPassthroughInput(it.id), TvContract.Channels.CONTENT_ITEM_TYPE))
        }.sortedBy { it.title }.toMutableList()
        val liveTv = context.getString(R.string.live_tv_package)
        if (liveTv.isNotBlank()) appIntent(liveTv)?.let { targets.add(InputTarget("live-tv", "Live TV", it)) }
        targets
    }
    fun appIntent(pkg: String): Intent? = (context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
        ?: context.packageManager.getLaunchIntentForPackage(pkg))?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val media = MatrixMediaRouter(context, prefs) { type, id, reference ->
        if (reference.isNotBlank()) simklGate.withLock { simkl.imdb(reference) }
        else if (id.startsWith("tmdb:")) search.imdbId(type, id.removePrefix("tmdb:"))
        else ""
    }
    suspend fun mediaIntent(card: MediaCard): Intent = media.intent(card)
    fun clear() { artwork.clear() }
}

internal fun filterApps(apps: List<LaunchApp>, query: String): List<LaunchApp> {
    val clean = query.trim()
    if (clean.isBlank()) return emptyList()
    return apps.filter { it.title.contains(clean, ignoreCase = true) }
        .sortedWith(
            compareByDescending<LaunchApp> { it.title.equals(clean, ignoreCase = true) }
                .thenByDescending { it.title.startsWith(clean, ignoreCase = true) }
                .thenBy { it.title.lowercase() }
        )
}
