package com.matrix.launcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Uses the same TV controls/cards as saved wallpapers, including remote focus and scrolling. */
@Composable
internal fun MatrixDevicePictures(page: String, repository: ComposeHomeRepository, push: (String) -> Unit,
    back: () -> Unit, changed: () -> Unit, notice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        revision++
        if (!DevicePictures.allowed(context)) notice("Photo access is off. Allow it in App permissions, or share an image from a file manager.")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    val allowed = remember(revision) { DevicePictures.allowed(context) }
    var failure by remember { mutableStateOf(false) }
    val pictures by produceState<List<DevicePicture>?>(null, revision) {
        value = null; failure = false
        try { value = DevicePictures.read(context) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SecurityException) { failure = true; value = emptyList() }
        catch (_: RuntimeException) { failure = true; value = emptyList() }
    }
    if (page == "wallpaper-device") {
        val albums = remember(pictures) { pictures.orEmpty().groupBy { it.albumId } }
        SettingsPage("Import pictures", "Browse with the remote. Photos are copied only when selected; your current wallpaper stays unchanged. Up to 500 recent indexed images, 10 MB each.",
            buildList {
                if (!allowed || failure) {
                    add(SettingAction("Allow photo access", "Required for this remote-friendly browser. Shared-image imports still work without this permission.") { permission.launch(DevicePictures.permissions()) })
                    add(SettingAction("App permissions", "If access was denied, open Permissions → Photos and videos") {
                        try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null))) }
                        catch (_: android.content.ActivityNotFoundException) { notice("Open Matrix's permissions from the TV's app settings.") }
                    })
                }
                else {
                    add(SettingAction("All pictures", if (pictures == null) "Loading…" else "${pictures!!.size} available", id = "all") { push("wallpaper-device:all") })
                    albums.forEach { (id, images) -> add(SettingAction(images.first().album, "${images.size} pictures", id = id) { push("wallpaper-device:$id") }) }
                    add(SettingAction("Refresh pictures", "Reload downloaded images and check photo access") { revision++ })
                    add(SettingAction("Change photo access", "Android may allow all photos or only selected pictures") { permission.launch(DevicePictures.permissions()) })
                }
            }, back)
        return
    }
    val album = page.substringAfter(':')
    val visible = remember(pictures, album) { pictures.orEmpty().filter { album == "all" || it.albumId == album } }
    val cards = remember(visible) { visible.map { MediaCard(it.uri, it.name, it.uri, it.uri, "matrix.wallpaper", false, 0, 0, "OK to import") } }
    val initial = remember(page) { FocusRequester() }
    val grid = rememberLazyGridState()
    Column {
        MatrixPageHeader(if (album == "all") "All pictures" else visible.firstOrNull()?.album ?: "Pictures", back, if (cards.isEmpty()) initial else null)
        when {
            pictures == null -> MatrixLoadState("Loading pictures…", true)
            cards.isEmpty() -> MatrixLoadState("No accessible pictures. Go back to allow photo access or share a downloaded image from a file manager.")
            else -> MatrixMediaGrid(cards, 3, grid, repository, cards.first().id, initial, { _, _ -> }, { card ->
                if (!busy) scope.launch {
                    busy = true
                    try {
                        val count = repository.wallpapers.import(listOf(card.link.toUri()))
                        notice(if (count > 0) "Picture imported. Choose saved wallpaper to use it." else "Cannot read this picture. Try a JPEG or PNG smaller than 10 MB.")
                        if (count > 0) { changed(); back() }
                    } finally { busy = false }
                }
            }, Modifier.weight(1f))
        }
    }
    LaunchedEffect(cards.map { it.id }) { withFrameNanos { }; initial.requestFocus() }
}
