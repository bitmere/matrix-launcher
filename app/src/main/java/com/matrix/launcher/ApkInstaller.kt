package com.matrix.launcher

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.security.MessageDigest

internal sealed interface InstallOutcome {
    data class SessionStarted(val sessionId: Int) : InstallOutcome
    data object SystemInstallerLaunched : InstallOutcome
    data object NeedsUnknownSources : InstallOutcome
    data class Error(val message: String) : InstallOutcome
}

/**
 * Installs a verified APK through a PackageInstaller session, with the
 * system installer as fallback. Always ask for confirmation; successful self
 * replacement kills this process, so the result receiver also reopens Home.
 * API 26–30 use the normal REQUEST_INSTALL_PACKAGES confirmation flow.
 */
internal object ApkInstaller {

    internal const val ACTION_INSTALL_STATUS = "com.matrix.launcher.UPDATE_INSTALL_STATUS"
    internal const val EXTRA_APK_PATH = "apk_path"
    internal const val EXTRA_VERSION_CODE = "version_code"
    internal const val EXTRA_OPERATION = "operation"

    fun canInstallUnknownSources(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /**
     * Install [apkFile]. Returns [InstallOutcome.NeedsUnknownSources] without
     * creating a session when the unknown-sources grant is missing; falls
     * back to the system installer if the session path throws.
     */
    fun install(context: Context, apkFile: File, release: LatestRelease, operation: String): InstallOutcome {
        val versionCode = release.versionCode
        if (!canInstallUnknownSources(context)) return InstallOutcome.NeedsUnknownSources
        if (!runCatching { matchesRelease(context, apkFile, release) }.getOrDefault(false)) {
            return InstallOutcome.Error("The APK identity, version or signing certificate is invalid. Install the approved release from GitHub.")
        }
        val savedSessionId = PendingUpdateSession.savedId(context)
        try { PendingUpdateSession.abandon(context) } catch (failure: Exception) {
            android.util.Log.w("MatrixUpdate",
                "Couldn’t settle saved session $savedSessionId: ${failure.javaClass.name}: ${failure.message}", failure)
            return InstallOutcome.Error("Couldn’t settle the previous install. Try again.")
        }
        return try {
            installViaSession(context, apkFile, versionCode, operation)
        } catch (e: Exception) {
            installViaSystemInstaller(context, apkFile)
        }
    }

    @Suppress("DEPRECATION")
    private fun matchesRelease(context: Context, file: File, release: LatestRelease): Boolean {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val flags = if (modern) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val packages = context.packageManager
        val archive = packages.getPackageArchiveInfo(file.absolutePath, flags) ?: return false
        val installed = packages.getPackageInfo(context.packageName, flags)
        if (archive.packageName != context.packageName || archive.versionName != release.versionName ||
            archive.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 ||
            (if (modern) archive.longVersionCode else archive.versionCode.toLong()) != release.versionCode ||
            !UpdateCheck.isNewer(release.versionCode, UpdateCheck.installedVersionCode(context))
        ) return false

        fun signers(info: PackageInfo): Set<String> {
            val signatures = if (modern) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 0xff) }
            }.toSet()
        }
        val approved = setOf("a21fc7cb0cf12ab08744e03a84fc45be7fd50aafb7397f054d250ce1a5151b9b")
        return context.packageName == "com.matrix.launcher" && signers(archive) == approved && signers(installed) == approved
    }

    private fun installViaSession(context: Context, apkFile: File, versionCode: Long, operation: String): InstallOutcome {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apkFile.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }
        val id = commitInstallSession(object : InstallSessionAdapter {
            private var session: PackageInstaller.Session? = null
            override fun create(): Int {
                return installer.createSession(params)
            }
            override fun open(id: Int) { session = installer.openSession(id) }
            override fun writeAndSync() {
                val opened = checkNotNull(session)
                opened.openWrite("base.apk", 0, apkFile.length()).use { output ->
                    apkFile.inputStream().use { it.copyTo(output) }
                    opened.fsync(output)
                }
            }
            override fun commit(id: Int) {
                PendingUpdateSession.save(context, id, versionCode, apkFile)
                val statusIntent = Intent(context, UpdateInstallResultReceiver::class.java).apply {
                    action = ACTION_INSTALL_STATUS
                    data = "matrix-update://session/$id".toUri()
                    putExtra(EXTRA_APK_PATH, apkFile.absolutePath)
                    putExtra(EXTRA_VERSION_CODE, versionCode)
                    putExtra(EXTRA_OPERATION, operation)
                }
                val status = PendingIntent.getBroadcast(context, id, statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                checkNotNull(session).commit(status.intentSender)
            }
            override fun abandon(id: Int) {
                PendingUpdateSession.clear(context, id)
                installer.abandonSession(id)
            }
            override fun close() { session?.close() }
        })
        return InstallOutcome.SessionStarted(id)
    }

    private fun installViaSystemInstaller(context: Context, apkFile: File): InstallOutcome {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallOutcome.SystemInstallerLaunched
        } catch (e: Exception) {
            InstallOutcome.Error("Couldn't start the installer. Try again.")
        }
    }
}
