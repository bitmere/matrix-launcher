package com.matrix.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** One manual update check's UI state, driven by [UpdateCheckController]. */
internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val installedVersion: String) : UpdateUiState
    data class Available(val release: LatestRelease) : UpdateUiState
    data class Downloading(val release: LatestRelease, val downloaded: Long, val total: Long?) : UpdateUiState
    data class ReadyToInstall(val release: LatestRelease, val apkFile: File) : UpdateUiState
    data object Installing : UpdateUiState
    data class WaitingForConfirmation(val release: LatestRelease, val apkFile: File) : UpdateUiState
    data class Installed(val versionName: String) : UpdateUiState
    data class NeedsUnknownSources(val release: LatestRelease, val apkFile: File) : UpdateUiState
    /** [retryInstall] is set when the install itself failed: Try again retries the install, not the check. */
    data class Error(val message: String, val retryInstall: Pair<LatestRelease, File>? = null) : UpdateUiState
}

/**
 * Plain state holder for the manual update check, owned by the About page.
 * No DI framework. [scope] must be a Main-dispatcher scope (the page's
 * rememberCoroutineScope): state writes happen on it, and all
 * network/download/install work runs on [Dispatchers.IO] inside it.
 */
internal class UpdateCheckController(
    context: Context?,
    private val scope: CoroutineScope,
    private val backend: UpdateBackend = AndroidUpdateBackend(requireNotNull(context))
) {
    var uiState by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
        private set

    private var job: Job? = null
    private var installOperation: String? = null
    private var disposed = false

    /** The About row's action: dispatches on the current state. */
    fun onRowAction() {
        if (disposed) return
        when (val state = uiState) {
            UpdateUiState.Idle, is UpdateUiState.UpToDate, is UpdateUiState.Installed -> check()
            is UpdateUiState.Error -> {
                val retry = state.retryInstall
                if (retry != null && retry.second.exists()) {
                    uiState = UpdateUiState.ReadyToInstall(retry.first, retry.second)
                } else {
                    check()
                }
            }
            is UpdateUiState.Available -> download()
            is UpdateUiState.Downloading -> cancelDownload()
            is UpdateUiState.ReadyToInstall -> install()
            is UpdateUiState.NeedsUnknownSources -> {
                if (backend.canInstall()) install()
                else openUnknownSourcesSettings()
            }
            is UpdateUiState.WaitingForConfirmation -> {
                if (state.apkFile.exists()) uiState = UpdateUiState.ReadyToInstall(state.release, state.apkFile)
                else check()
            }
            UpdateUiState.Checking, UpdateUiState.Installing -> { /* in flight: no-op */ }
        }
    }

    fun check() {
        if (uiState is UpdateUiState.Checking || uiState is UpdateUiState.Downloading) return
        job?.cancel()
        uiState = UpdateUiState.Checking
        job = scope.launch {
            val (result, installedName) = withContext(Dispatchers.IO) { backend.check() to backend.installedName }
            uiState = when (result) {
                is UpdateCheckResult.Available -> UpdateUiState.Available(result.release)
                UpdateCheckResult.UpToDate ->
                    UpdateUiState.UpToDate(installedName)
                is UpdateCheckResult.Error -> UpdateUiState.Error(result.message)
            }
        }
    }

    fun download() {
        val release = (uiState as? UpdateUiState.Available)?.release ?: return
        job?.cancel()
        uiState = UpdateUiState.Downloading(release, 0, null)
        job = scope.launch {
            var ownedFile: File? = null
            try {
                val file = withContext(Dispatchers.IO) {
                    var lastPercent = -1
                    var lastPostedBytes = 0L
                    backend.download(release) { downloaded, total ->
                        val percent = total?.let { if (it > 0) (downloaded * 100 / it).toInt() else null }
                        // Progress posts to Main, throttled: on percent change, or every
                        // 512 KB when the total is unknown.
                        val shouldPost = if (percent != null) percent != lastPercent
                        else downloaded - lastPostedBytes >= 512 * 1024
                        if (shouldPost) {
                            lastPercent = percent ?: -1
                            lastPostedBytes = downloaded
                            scope.launch { updateProgress(release, downloaded, total) }
                        }
                    }.also { ownedFile = it }
                }
                currentCoroutineContext().ensureActive()
                uiState = UpdateUiState.ReadyToInstall(release, file)
            } catch (e: CancellationException) {
                ownedFile?.delete()
                // Cancel returns to the update-available state; the partial file is already deleted.
                uiState = UpdateUiState.Available(release)
            } catch (e: UpdateVerificationException) {
                uiState = UpdateUiState.Error(e.message ?: "The downloaded file didn't match the published hash.")
            } catch (e: IOException) {
                uiState = UpdateUiState.Error("Download failed. Check your connection and try again.")
            }
        }
    }

    fun cancelDownload() {
        if (uiState is UpdateUiState.Downloading) job?.cancel()
    }

    fun install() {
        val (release, apkFile) = when (val state = uiState) {
            is UpdateUiState.ReadyToInstall -> state.release to state.apkFile
            is UpdateUiState.NeedsUnknownSources -> state.release to state.apkFile
            else -> return
        }
        if (!backend.canInstall()) {
            uiState = UpdateUiState.NeedsUnknownSources(release, apkFile)
            return
        }
        uiState = UpdateUiState.Installing
        val operation = java.util.UUID.randomUUID().toString()
        installOperation = operation
        UpdateInstallEvents.setListener(operation) { result ->
            when (result) {
                UpdateInstallEvents.InstallResult.Installed ->
                    uiState = UpdateUiState.Installed(release.versionName)
                UpdateInstallEvents.InstallResult.WaitingForConfirmation ->
                    uiState = UpdateUiState.WaitingForConfirmation(release, apkFile)
                is UpdateInstallEvents.InstallResult.Failed ->
                    uiState = UpdateUiState.Error(result.message, retryInstall = release to apkFile)
            }
        }
        job = scope.launch {
            val outcome = withContext(Dispatchers.IO) { backend.install(apkFile, release, operation) }
            withContext(scope.coroutineContext) {
                if (disposed || installOperation != operation) return@withContext
                if (outcome !is InstallOutcome.SessionStarted) UpdateInstallEvents.clear(operation)
                when (outcome) {
                    is InstallOutcome.SessionStarted -> { /* result arrives via the receiver */ }
                    InstallOutcome.SystemInstallerLaunched ->
                        uiState = UpdateUiState.WaitingForConfirmation(release, apkFile)
                    InstallOutcome.NeedsUnknownSources ->
                        uiState = UpdateUiState.NeedsUnknownSources(release, apkFile)
                    is InstallOutcome.Error -> uiState = UpdateUiState.Error(outcome.message, retryInstall = release to apkFile)
                }
            }
        }
    }

    fun openUnknownSourcesSettings() {
        val pending = uiState as? UpdateUiState.NeedsUnknownSources ?: return
        if (!backend.openSettings()) {
            uiState = UpdateUiState.Error("Couldn’t open install settings. Try again.", pending.release to pending.apkFile)
        }
    }

    /** Returning from permission settings offers Install and never starts it automatically. */
    fun onResume() {
        val pending = uiState as? UpdateUiState.NeedsUnknownSources ?: return
        if (backend.canInstall()) uiState = UpdateUiState.ReadyToInstall(pending.release, pending.apkFile)
    }

    private fun updateProgress(release: LatestRelease, downloaded: Long, total: Long?) {
        if (uiState is UpdateUiState.Downloading) {
            uiState = UpdateUiState.Downloading(release, downloaded, total)
        }
    }

    /** Call when the About page leaves composition: stops in-flight work and drops the install listener. */
    fun dispose() {
        job?.cancel()
        disposed = true
        installOperation?.let { UpdateInstallEvents.clear(it) }
    }
}
