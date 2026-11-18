package com.matrix.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import java.io.File

/** Small boundary for deterministic controller tests without Android or a network. */
internal interface UpdateBackend {
    val installedName: String
    fun canInstall(): Boolean
    fun openSettings(): Boolean
    suspend fun check(): UpdateCheckResult
    suspend fun download(release: LatestRelease, progress: (Long, Long?) -> Unit): File
    fun install(file: File, release: LatestRelease, operation: String): InstallOutcome
}

internal class AndroidUpdateBackend(private val context: Context) : UpdateBackend {
    override val installedName get() = UpdateCheck.installedVersionName(context)
    override fun canInstall() = ApkInstaller.canInstallUnknownSources(context)
    override suspend fun check() = UpdateCheck.check(context)
    override suspend fun download(release: LatestRelease, progress: (Long, Long?) -> Unit) =
        ApkDownload.download(context.filesDir, release, onProgress = progress)
    override fun install(file: File, release: LatestRelease, operation: String) = ApkInstaller.install(context, file, release, operation)
    override fun openSettings(): Boolean {
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = "package:${context.packageName}".toUri() },
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        )
        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) == null) continue
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) { /* Some firmware denies a screen that resolves. Try the fallback. */ }
        }
        return false
    }
}
