package com.matrix.launcher

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Thrown when a downloaded APK fails its SHA-256 check. The file is already deleted. */
internal class UpdateVerificationException(message: String) : IOException(message)

/** Streams and verifies one update at a time in the app's private directory. */
internal object ApkDownload {
    private val gate = Mutex()
    internal const val MAX_APK_BYTES = 256L * 1024 * 1024
    private val sidecar = Regex("([0-9a-f]{64})  " + Regex.escape(UPDATE_APK) + "\n")

    fun updatesDir(filesDir: File): File = File(filesDir, "updates").apply { mkdirs() }

    internal fun parseChecksum(bytes: ByteArray): String? =
        sidecar.matchEntire(bytes.toString(Charsets.US_ASCII))?.groupValues?.get(1)

    private val hashPattern = Regex("[0-9a-f]{64}")

    /** Resolves the expected SHA-256 for [release]: the notes hash when
     * present (no network request), else the strict sidecar fetch, else
     * an error so no unverifiable download starts. */
    internal fun checksum(
        release: LatestRelease,
        connect: (String) -> HttpURLConnection,
        active: () -> Unit
    ): String {
        if (release.sha256.isNotEmpty()) {
            if (!hashPattern.matches(release.sha256)) {
                throw UpdateVerificationException("The release checksum is invalid.")
            }
            return release.sha256
        }
        if (release.checksumUrl.isEmpty()) throw IOException("The update has no published checksum.")
        val tag = "v${release.versionName}"
        return readReleaseAsset(release.checksumUrl, tag, UPDATE_CHECKSUM, connect, active) { connection ->
            checkLength(connection, CHECKSUM_BYTES.toLong())
            val bytes = connection.inputStream.use { readBounded(it, CHECKSUM_BYTES, active) }
            parseChecksum(bytes) ?: throw UpdateVerificationException("The release checksum is invalid.")
        }
    }

    suspend fun download(
        filesDir: File,
        release: LatestRelease,
        connect: (String) -> HttpURLConnection = { PersonalImages.connect(it, null) },
        maxBytes: Long = MAX_APK_BYTES,
        onProgress: (downloaded: Long, total: Long?) -> Unit
    ): File = gate.withLock {
        val operation = coroutineContext
        val active = { operation.ensureActive() }
        val tag = "v${release.versionName}"
        val expectedSize = release.apkSize
        if (expectedSize !in 1..minOf(maxBytes, MAX_APK_BYTES)) throw IOException("Invalid update size.")
        val expectedHash = checksum(release, connect, active)

        val dir = updatesDir(filesDir)
        val target = File.createTempFile("Matrix-${release.versionCode}-", ".apk", dir)
        var verified = false
        try {
            readReleaseAsset(release.apkUrl, tag, UPDATE_APK, connect, active) { connection ->
                checkLength(connection, expectedSize)
                val hash = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                connection.inputStream.use { input ->
                    DigestOutputStream(target.outputStream(), hash).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            active()
                            val count = input.read(buffer)
                            if (count == -1) break
                            downloaded += count
                            if (downloaded > expectedSize) throw IOException("The update file exceeds its published size.")
                            output.write(buffer, 0, count)
                            onProgress(downloaded, expectedSize)
                        }
                    }
                }
                active()
                if (downloaded != expectedSize) throw IOException("The update file is shorter than its published size.")
                if (hex(hash.digest()) != expectedHash) {
                    throw UpdateVerificationException("The downloaded file didn't match the published hash.")
                }
            }
            verified = true
            target
        } finally {
            if (!verified) target.delete()
        }
    }

    private fun checkLength(connection: HttpURLConnection, expected: Long) {
        val length = connection.contentLengthLong
        if (length >= 0 && length != expected) throw IOException("Unexpected download length.")
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 0xFF) }

    /** Streaming SHA-256 of a file, lower-case hex. */
    internal fun sha256Hex(file: File, checkActive: () -> Unit = {}): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (true) {
                checkActive()
                read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
            checkActive()
        }
        return hex(digest.digest())
    }

    /** True only when the file's SHA-256 equals [expectedSha256] (case-insensitive). */
    internal fun verify(file: File, expectedSha256: String): Boolean =
        runCatching { sha256Hex(file) }.getOrNull() == expectedSha256.lowercase()
}
