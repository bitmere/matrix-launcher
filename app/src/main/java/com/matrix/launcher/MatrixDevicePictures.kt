package com.matrix.launcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Uses the same TV controls/cards as saved wallpapers, including remote focus and scrolling. */
@Composable
internal fun MatrixDevicePictures(page: String, repository: ComposeHomeRepository, windowWidth: androidx.compose.ui.unit.Dp, push: (String) -> Unit,
    back: () -> Unit, changed: () -> Unit, notice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLabel = DevicePictures.permissionLabel()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        revision++
        permanentlyDenied = !DevicePictures.allowed(context) &&
            (context as? android.app.Activity)?.let { activity ->
                DevicePictures.permissions().none { activity.shouldShowRequestPermissionRationale(it) }
            } == true
        if (!DevicePictures.allowed(context)) notice("$permissionLabel access is off. Use App permissions, Choose image, or share an image into Matrix.")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    val allowed = remember(revision) { DevicePictures.allowed(context) }
    var permissionLost by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    val pictures by produceState<List<DevicePicture>?>(null, revision) {
        value = null; failure = false; permissionLost = false
        try { value = DevicePictures.read(context) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SecurityException) { permissionLost = !DevicePictures.allowed(context); failure = !permissionLost; value = emptyList() }
        catch (_: RuntimeException) { failure = true; value = emptyList() }
    }
    DisposableEffect(Unit) {
        val cancel = MatrixSaf.cancelPending
        onDispose { cancel?.invoke() }
    }
    fun importPicture(uri: android.net.Uri) {
        if (!busy) scope.launch {
            busy = true
            try {
                val count = repository.wallpapers.import(listOf(uri))
                notice(if (count > 0) "Picture imported. Choose saved wallpaper to use it."
                    else "Cannot read this image. It may be unavailable, unsupported, or larger than 10 MB. Try another image.")
                if (count > 0) changed()
            } finally { busy = false }
        }
    }
    if (page == "wallpaper-device") {
        val albums = remember(pictures) { pictures.orEmpty().groupBy { it.albumId } }
        SettingsPage("Import pictures", "Browse with the remote. Photos are copied only when selected; your current wallpaper stays unchanged. Up to 500 recent indexed images, 10 MB each.",
            buildList {
                if (!allowed || permissionLost) {
                    if (!permanentlyDenied) add(SettingAction("Allow $permissionLabel access", "Required to browse TV pictures. Choose image and shared-image imports do not need this permission.") { permission.launch(DevicePictures.permissions()) })
                    else add(SettingAction("$permissionLabel access denied", "Android is no longer showing the permission prompt. Use App permissions or Choose image.", isStatus = true))
                    add(SettingAction("App permissions", "Open Permissions → $permissionLabel (wording may vary on this TV)") {
                        try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null))) }
                        catch (_: android.content.ActivityNotFoundException) { notice("Open Matrix's permissions from the TV's app settings.") }
                        catch (_: SecurityException) { notice("This TV blocked app settings. Open Matrix's permissions from TV settings.") }
                    })
                }
                else {
                    if (failure) add(SettingAction("Cannot load TV pictures", "The image index could not be read. Refresh to retry, choose an image, or share one into Matrix.", isStatus = true))
                    else if (pictures?.isEmpty() == true) add(SettingAction("No indexed pictures", "The TV image index has no accessible images within the 10 MB limit. Choose image or share an image into Matrix.", isStatus = true))
                    else add(SettingAction("All pictures", if (pictures == null) "Loading…" else "${pictures!!.size} available", id = "all") { push("wallpaper-device:all") })
                    albums.forEach { (id, images) -> add(SettingAction(images.first().album, "${images.size} pictures", id = id) { push("wallpaper-device:$id") }) }
                    add(SettingAction("Refresh pictures", "Reload downloaded images and check photo access") { revision++ })
                    add(SettingAction("Change $permissionLabel access", "Choose which pictures Matrix may browse in the system permission dialog") { permission.launch(DevicePictures.permissions()) })
                }
                add(SettingAction(stringResource(R.string.wallpaper_choose_image),
                    stringResource(R.string.wallpaper_choose_detail), role = MatrixControlRole.Action) {
                    if (!busy) MatrixSaf.notice(context, MatrixSaf.open(arrayOf("image/*")) { uri ->
                        uri?.let(::importPicture)
                    }, notice)
                })
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
            !allowed || permissionLost -> MatrixLoadState("$permissionLabel access is off. Go back to App permissions or Choose image.")
            failure -> MatrixLoadState("Cannot load the TV image index. Go back and refresh, or choose an image.")
            cards.isEmpty() -> MatrixLoadState("No indexed pictures in this album within the 10 MB limit. Go back to Choose image, or share an image into Matrix.")
            else -> MatrixMediaGrid(cards, windowWidth, 3, grid, repository, cards.first().id, initial, { _, _ -> }, { card ->
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
