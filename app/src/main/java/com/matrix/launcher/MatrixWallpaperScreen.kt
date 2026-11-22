package com.matrix.launcher

import androidx.compose.runtime.*
import androidx.core.content.edit
import kotlinx.coroutines.launch

@Composable
internal fun MatrixWallpaperScreen(page: String, repository: ComposeHomeRepository, windowWidth: androidx.compose.ui.unit.Dp, push: (String) -> Unit,
    back: () -> Unit, changed: () -> Unit, notice: (String) -> Unit) {
    val prefs = repository.prefs
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    fun save(block: android.content.SharedPreferences.Editor.() -> Unit) { prefs.edit { block() }; revision++; changed() }
    revision
    when (page) {
        "wallpapers" -> SettingsPage("Wallpapers", if (busy) "Working with your local collection…" else "Import your own pictures or download Commons landscapes. Up to 20 imports and 20 Commons images, stored at Full HD for offline rotation. Earlier saved pictures are kept.", listOf(
            SettingAction("Import pictures", "Remote-friendly albums and pictures on this TV", MatrixControlRole.Action) { push("wallpaper-device") },
            SettingAction("Choose saved wallpaper", "Browse imported pictures and saved Commons landscapes") { push("wallpaper-gallery") },
            SettingAction("Source · ${prefs.getString("wallpaper_mode", "both")}", "Use pictures or the focused artwork's ambient color", id = "wallpaper-source") { push("wallpaper-source") },
            SettingAction("Next wallpaper", role = MatrixControlRole.Action) { if (!busy) save { putLong("wallpaper_changed", 0) } },
            SettingAction("Rotate · ${prefs.getInt("rotation_minutes", 30)} minutes", id = "wallpaper-interval") { push("wallpaper-interval") },
            SettingAction("Daily Commons downloads", "Featured landscapes; no account or API key. Checks while the launcher is visible, at most once daily.", checked = prefs.getBoolean("commons_enabled", false)) { save { putBoolean("commons_enabled", !prefs.getBoolean("commons_enabled", false)) } },
            SettingAction("Download landscapes", prefs.getString("commons_status", null) ?: "Adds up to five pictures; retains saved images when offline", MatrixControlRole.Action) {
                if (!busy) scope.launch { busy = true; try { repository.wallpapers.refresh(true)?.let(notice); changed() } finally { busy = false } }
            },
            SettingAction("Wallpaper credits", "Creators, licenses and original Commons file pages") { push("wallpaper-credits") }
        ), back)
        "wallpaper-device" -> MatrixDevicePictures(page, repository, windowWidth, push, back, changed, notice)
        "wallpaper-gallery" -> MatrixSavedWallpapers(repository, windowWidth, back, changed, notice)
        "wallpaper-source" -> SettingsPage("Wallpaper source", "With no pictures, Home uses the focused app or poster color.",
            listOf("both" to "All saved pictures", "local" to "Imported and earlier saved pictures", "downloaded" to "Commons downloads only", "off" to "Focused artwork color").map { (id, name) ->
                SettingAction(name, selected = prefs.getString("wallpaper_mode", "both") == id, id = id) { save { putString("wallpaper_mode", id); putLong("wallpaper_changed", 0) }; back() }
            }, back)
        "wallpaper-interval" -> SettingsPage("Rotation interval", "No background polling service.", listOf(15, 30, 60, 120).map { minutes ->
            SettingAction("$minutes minutes", selected = prefs.getInt("rotation_minutes", 30) == minutes) { save { putInt("rotation_minutes", minutes) }; back() }
        }, back)
        "wallpaper-credits" -> {
            val credits by produceState<List<WallpaperCredit>?>(null) { value = repository.wallpapers.credits() }
            if (credits.isNullOrEmpty()) MatrixStatusPage("Wallpaper credits",
                if (credits == null) "Loading credits…" else "No Commons downloads yet. Download landscapes to see their credits here.", credits == null, back)
            else SettingsPage("Wallpaper credits", "Downloaded images are resized and JPEG-encoded. Source links and original licenses are retained with each image.",
                credits.orEmpty().map { credit -> SettingAction(credit.title, "${credit.artist} · ${credit.license} · Resized", id = credit.id) { push("wallpaper-credit:${credit.id}") } }, back)
        }
        // Old restored navigation entries must not revive removed credential forms.
        else -> if (page.startsWith("wallpaper-credit:")) {
            val credit by produceState<WallpaperCredit?>(null, page) {
                value = repository.wallpapers.credits().firstOrNull { it.id == page.substringAfter(':') }
            }
            if (credit != null) MatrixQrPage("Wallpaper credit", credit!!.page,
                "${credit!!.title}\n${credit!!.artist} · ${credit!!.license}\nResized and JPEG-encoded. Scan for original file, full attribution and license.", back)
            else SettingsPage("Wallpaper credit", "Loading credit, or this picture is no longer in the cache.", emptyList(), back)
        } else if (page.startsWith("wallpaper-device:")) MatrixDevicePictures(page, repository, windowWidth, push, back, changed, notice)
        else SettingsPage("Wallpapers", "Reddit integration has been removed. Saved pictures are kept offline.",
                listOf(SettingAction("Wallpaper settings", role = MatrixControlRole.Action) { back() }), back)
    }
}

@Composable
private fun MatrixSavedWallpapers(repository: ComposeHomeRepository, windowWidth: androidx.compose.ui.unit.Dp, back: () -> Unit, changed: () -> Unit, notice: (String) -> Unit) {
    val files by produceState<List<java.io.File>?>(null, repository) { value = repository.wallpapers.collection() }
    val scope = rememberCoroutineScope()
    val initial = remember { androidx.compose.ui.focus.FocusRequester() }
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val current = repository.prefs.getString("wallpaper_current", "")
    val cards = remember(files, current) { files.orEmpty().mapIndexed { index, file -> MediaCard(file.absolutePath, "Picture ${index + 1}", file.toURI().toString(), file.absolutePath,
        "matrix.wallpaper", false, 0, 0, if (file.absolutePath == current) "Current wallpaper" else "") } }
    androidx.compose.foundation.layout.Column {
        MatrixPageHeader("Saved wallpapers", back, if (cards.isEmpty()) initial else null)
        when {
            files == null -> MatrixLoadState("Loading saved pictures…", true)
            cards.isEmpty() -> MatrixLoadState("No saved pictures. Import an image or download Commons landscapes first.")
            else -> MatrixMediaGrid(cards, windowWidth, 3, grid, repository,
                cards.firstOrNull { it.id == repository.prefs.getString("wallpaper_current", "") }?.id ?: cards.first().id,
                initial, { _, _ -> }, { card -> scope.launch {
                    if (repository.wallpapers.select(card.link)) { changed(); back() }
                    else notice("This picture is no longer available.")
                } }, modifier = androidx.compose.ui.Modifier.weight(1f))
        }
    }
    LaunchedEffect(cards.map { it.id }) {
        if (cards.isNotEmpty()) {
            val index = cards.indexOfFirst { it.id == repository.prefs.getString("wallpaper_current", "") }.coerceAtLeast(0)
            grid.scrollToItem(index)
        }
        withFrameNanos { };initial.requestFocus()
    }
}
