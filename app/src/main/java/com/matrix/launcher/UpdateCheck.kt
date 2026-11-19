package com.matrix.launcher

import android.content.Context
import android.os.Build
import kotlinx.coroutines.ensureActive
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.SocketTimeoutException
import kotlin.coroutines.coroutineContext

internal const val UPDATE_REPO = "bitmere/matrix-launcher"
internal const val UPDATE_LATEST_URL = "https://api.github.com/repos/$UPDATE_REPO/releases/latest"
internal const val UPDATE_APK = "Matrix-Launcher.apk"
internal const val UPDATE_CHECKSUM = "$UPDATE_APK.sha256"
internal const val CHECKSUM_BYTES = 64 + 2 + UPDATE_APK.length + 1

internal data class LatestRelease(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val apkSize: Long,
    val checksumUrl: String,
    val notes: String,
    val sha256: String
)

internal sealed interface UpdateCheckResult {
    data class Available(val release: LatestRelease) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

internal object UpdateCheck {
    private val stableTag = Regex("v[0-9]+\\.[0-9]+\\.[0-9]+")
    private val versionLine = Regex("Android-Version-Code: ([1-9][0-9]*)")
    private val hashLine = Regex("SHA-256: ([0-9a-f]{64})")
    private class InvalidAssets : IOException()

    fun parseRelease(json: String): LatestRelease {
        val release = JSONObject(json)
        fun string(key: String) = release.get(key) as? String ?: throw JSONException("Invalid $key")
        val tag = string("tag_name")
        if (!stableTag.matches(tag) || release.get("draft") != false || release.get("prerelease") != false ||
            string("name").isBlank() || string("html_url") != "https://github.com/$UPDATE_REPO/releases/tag/$tag"
        ) throw JSONException("Invalid stable release")
        val lines = string("body").trimEnd('\r', '\n').lines()
        val code = versionLine.matchEntire(lines.last())?.groupValues?.get(1)?.toLongOrNull()
        if (code == null || lines.dropLast(1).any { it.trimStart().startsWith("Android-Version-Code", ignoreCase = true) }) {
            throw JSONException("Invalid versionCode trailer")
        }
        // At most one SHA-256 line, and when present it must be the penultimate
        // line matching exactly, with no leading or trailing whitespace.
        val hashCount = lines.count { it.trimStart().startsWith("SHA-256:") }
        val sha256 = if (hashCount == 0) "" else {
            if (hashCount != 1 || lines.size < 2) throw JSONException("Invalid SHA-256 line in release notes")
            hashLine.matchEntire(lines[lines.lastIndex - 1])?.groupValues?.get(1)
                ?: throw JSONException("Invalid SHA-256 line in release notes")
        }

        val assets = release.optJSONArray("assets") ?: throw InvalidAssets()
        val selected = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
        fun asset(name: String, max: Long): Pair<String, Long> {
            val match = selected.singleOrNull { it.opt("name") == name } ?: throw InvalidAssets()
            val size = when (val raw = match.opt("size")) {
                is Int -> raw.toLong()
                is Long -> raw
                else -> throw InvalidAssets()
            }
            val url = match.opt("browser_download_url") as? String ?: throw InvalidAssets()
            if (match.opt("state") != "uploaded" || size !in 1..max ||
                url != "https://github.com/$UPDATE_REPO/releases/download/$tag/$name"
            ) throw InvalidAssets()
            return url to size
        }
        val (apkUrl, apkSize) = asset(UPDATE_APK, ApkDownload.MAX_APK_BYTES)
        // The sidecar is optional: older releases (e.g. 2.7.6) have no notes
        // hash, so a missing sidecar parses with an empty checksumUrl and the
        // download step then refuses the unverifiable update. When present it
        // is validated exactly as before.
        val checksumUrl = if (selected.any { it.opt("name") == UPDATE_CHECKSUM }) {
            val (url, size) = asset(UPDATE_CHECKSUM, CHECKSUM_BYTES.toLong())
            if (size != CHECKSUM_BYTES.toLong()) throw InvalidAssets()
            url
        } else ""
        return LatestRelease(tag.removePrefix("v"), code, apkUrl, apkSize, checksumUrl,
            lines.dropLast(1 + hashCount).joinToString("\n").trimEnd(), sha256)
    }

    fun isNewer(remoteVersionCode: Long, installedVersionCode: Long): Boolean =
        remoteVersionCode > installedVersionCode

    @Suppress("DEPRECATION")
    fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else info.versionCode.toLong()
    }

    @Suppress("DEPRECATION")
    fun installedVersionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    suspend fun check(context: Context): UpdateCheckResult {
        val installed = runCatching { installedVersionCode(context) }.getOrNull()
            ?: return UpdateCheckResult.Error("Couldn't read the installed version.")
        return check(installed)
    }

    suspend fun check(
        installed: Long,
        connect: (String) -> HttpURLConnection = { PersonalImages.connect(it, null) }
    ): UpdateCheckResult {
        val operation = coroutineContext
        val release = try {
            operation.ensureActive()
            val connection = connect(UPDATE_LATEST_URL)
            try {
                when (val status = connection.responseCode) {
                    200 -> parseRelease(connection.inputStream.use {
                        readBounded(it, 256 * 1024) { operation.ensureActive() }.toString(Charsets.UTF_8)
                    })
                    403, 429 -> return UpdateCheckResult.Error("GitHub refused or rate-limited the check. Try again later.")
                    404 -> return UpdateCheckResult.Error("The stable release is unavailable. Try again later.")
                    else -> return UpdateCheckResult.Error("GitHub update check failed (HTTP $status).")
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: InvalidAssets) {
            return UpdateCheckResult.Error("The release has invalid update assets.")
        } catch (_: JSONException) {
            return UpdateCheckResult.Error("The release metadata is malformed.")
        } catch (_: BodyTooLarge) {
            return UpdateCheckResult.Error("The release metadata exceeds the size limit.")
        } catch (_: SocketTimeoutException) {
            return UpdateCheckResult.Error("The update check timed out. Try again.")
        } catch (_: IOException) {
            return UpdateCheckResult.Error("Couldn't check for updates. Check your connection and try again.")
        }
        operation.ensureActive()
        return if (isNewer(release.versionCode, installed)) UpdateCheckResult.Available(release)
        else UpdateCheckResult.UpToDate
    }
}

internal class BodyTooLarge : IOException("Response exceeded its size limit.")

internal fun readBounded(input: InputStream, limit: Int, checkActive: () -> Unit): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(minOf(limit + 1, 8192))
    while (true) {
        checkActive()
        val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
        if (count == -1) return output.toByteArray()
        if (count > limit - output.size()) throw BodyTooLarge()
        output.write(buffer, 0, count)
    }
}

internal fun <T> readReleaseAsset(
    initialUrl: String,
    tag: String,
    name: String,
    connect: (String) -> HttpURLConnection,
    checkActive: () -> Unit,
    read: (HttpURLConnection) -> T
): T {
    if (!Regex("v[0-9]+\\.[0-9]+\\.[0-9]+").matches(tag) ||
        initialUrl != "https://github.com/$UPDATE_REPO/releases/download/$tag/$name"
    ) throw IOException("Invalid release asset URL.")
    var url = initialUrl
    repeat(4) {
        checkActive()
        val connection = connect(url)
        try {
            when (val status = connection.responseCode) {
                200 -> return read(connection)
                301, 302, 303, 307, 308 -> {
                    val next = runCatching { URI(url).resolve(connection.getHeaderField("Location")) }.getOrNull()
                    if (next == null || next.scheme != "https" || next.host != "release-assets.githubusercontent.com" ||
                        next.rawUserInfo != null || next.port != -1 || next.rawFragment != null
                    ) throw IOException("Invalid release asset redirect.")
                    url = next.toASCIIString()
                }
                else -> throw IOException("Download failed (HTTP $status).")
            }
        } finally {
            connection.disconnect()
        }
    }
    throw IOException("Download redirected too many times.")
}
