package com.matrix.launcher

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "v2.7.6"
internal const val TEST_RELEASE_DOWNLOAD = "https://github.com/$UPDATE_REPO/releases/download/$TAG"

class UpdateCheckTest {
    private fun asset(name: String, size: Number) = JSONObject()
        .put("name", name).put("size", size).put("state", "uploaded")
        .put("browser_download_url", "$TEST_RELEASE_DOWNLOAD/$name")

    private fun document() = document(body = "Reviewed notes\n\nAndroid-Version-Code: 155")

    private fun document(body: String, sidecar: Boolean = true) = JSONObject()
        .put("tag_name", TAG).put("name", "Matrix Launcher $TAG")
        .put("html_url", "https://github.com/$UPDATE_REPO/releases/tag/$TAG")
        .put("draft", false).put("prerelease", false)
        .put("body", body)
        .put("assets", JSONArray().put(asset(UPDATE_APK, 3))
            .apply { if (sidecar) put(asset(UPDATE_CHECKSUM, CHECKSUM_BYTES)) })

    @Test fun parsesStableRelease() {
        assertEquals(
            LatestRelease("2.7.6", 155, "$TEST_RELEASE_DOWNLOAD/$UPDATE_APK", 3,
                "$TEST_RELEASE_DOWNLOAD/$UPDATE_CHECKSUM", "Reviewed notes", ""),
            UpdateCheck.parseRelease(document().toString())
        )
    }

    @Test fun parsesNotesHashAndOptionalSidecarIndependently() {
        val hash = "a".repeat(64)
        for (hasHash in listOf(false, true)) {
            for (hasSidecar in listOf(false, true)) {
                val body = "Reviewed notes\n\n" +
                    (if (hasHash) "SHA-256: $hash\n" else "") + "Android-Version-Code: 155"
                val release = UpdateCheck.parseRelease(document(body, hasSidecar).toString())
                assertEquals(if (hasHash) hash else "", release.sha256)
                assertEquals(if (hasSidecar) "$TEST_RELEASE_DOWNLOAD/$UPDATE_CHECKSUM" else "", release.checksumUrl)
                assertEquals("Reviewed notes", release.notes)
            }
        }
    }

    @Test fun rejectsMalformedNotesHashesEvenWithSidecar() {
        val hash = "a".repeat(64)
        val hashLine = "SHA-256: $hash"
        for (line in listOf(hashLine.dropLast(1), "SHA-256: ${hash.uppercase()}", "$hashLine ",
            "SHA-256:  $hash", "SHA-256:", "$hashLine\n$hashLine", "$hashLine\nMore notes", " $hashLine")) {
            val body = "Reviewed notes\n\n$line\nAndroid-Version-Code: 155"
            assertTrue(line, runCatching { UpdateCheck.parseRelease(document(body).toString()) }
                .exceptionOrNull() is org.json.JSONException)
        }
    }

    @Test fun rejectsWrongSidecarSizeEvenWithNotesHash() {
        val hash = "a".repeat(64)
        val body = "Reviewed notes\n\nSHA-256: $hash\nAndroid-Version-Code: 155"
        val assets = JSONArray().put(asset(UPDATE_APK, 3)).put(asset(UPDATE_CHECKSUM, CHECKSUM_BYTES - 1))
        assertTrue(runCatching { UpdateCheck.parseRelease(document(body).put("assets", assets).toString()) }
            .exceptionOrNull() is java.io.IOException)
    }

    @Test fun rejectsInvalidReleaseMetadata() {
        val invalid = listOf(
            document().put("draft", true),
            document().put("prerelease", true),
            document().put("tag_name", "v2.7.6-beta.1"),
            document().put("html_url", "https://example.invalid/releases/tag/$TAG"),
            document().put("body", "No trailer"),
            document().put("body", "Android-Version-Code: 155\nAndroid-Version-Code: 155"),
            document().put("body", "Android-Version-Code: abc"),
            document().put("body", "Android-Version-Code: 155\nMore notes")
        )
        for (release in invalid) {
            assertThrows(org.json.JSONException::class.java) { UpdateCheck.parseRelease(release.toString()) }
        }
    }

    @Test fun rejectsMissingDuplicateOrInvalidAssets() {
        fun checksum() = asset(UPDATE_CHECKSUM, CHECKSUM_BYTES)
        val apk = asset(UPDATE_APK, 3)
        val invalid = listOf(
            JSONArray().put(checksum()),
            JSONArray().put(apk).put(apk).put(checksum()),
            JSONArray().put(asset(UPDATE_APK, 3).put("browser_download_url", "https://example.invalid/$UPDATE_APK")).put(checksum()),
            JSONArray().put(asset(UPDATE_APK, 3).put("state", "starter")).put(checksum()),
            JSONArray().put(asset(UPDATE_APK, 0)).put(checksum()),
            JSONArray().put(asset(UPDATE_APK, ApkDownload.MAX_APK_BYTES + 1)).put(checksum()),
            JSONArray().put(apk).put(asset(UPDATE_CHECKSUM, CHECKSUM_BYTES - 1)),
            JSONArray().put(apk).put(asset(UPDATE_CHECKSUM, CHECKSUM_BYTES + 1))
        )
        for (assets in invalid) {
            assertThrows(java.io.IOException::class.java) {
                UpdateCheck.parseRelease(document().put("assets", assets).toString())
            }
        }
    }

    @Test fun comparesVersionCodes() {
        assertTrue(UpdateCheck.isNewer(155, 154))
        assertFalse(UpdateCheck.isNewer(155, 155))
        assertFalse(UpdateCheck.isNewer(155, 156))
        assertTrue(UpdateCheck.isNewer(Long.MAX_VALUE, Int.MAX_VALUE.toLong()))
    }

    private class Connection(private val status: Int, private val body: String) :
        HttpURLConnection(URL(UPDATE_LATEST_URL)) {
        var closed = false
        override fun connect() = Unit
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
    }

    @Test fun checkMapsStatusAndClosesConnection() = runBlocking {
        suspend fun check(status: Int, installed: Long): UpdateCheckResult {
            val connection = Connection(status, document().toString())
            val result = UpdateCheck.check(installed) { url ->
                assertEquals(UPDATE_LATEST_URL, url)
                connection
            }
            assertTrue(connection.closed)
            return result
        }
        assertTrue(check(200, 154) is UpdateCheckResult.Available)
        assertEquals(UpdateCheckResult.UpToDate, check(200, 155))
        for ((status, text) in listOf(403 to "rate-limited", 429 to "rate-limited", 404 to "unavailable", 500 to "HTTP 500")) {
            assertTrue((check(status, 154) as UpdateCheckResult.Error).message.contains(text))
        }
    }

    @Test fun checkOffersNotesHashReleaseWithoutChecksumFetch() = runBlocking {
        val hash = "a".repeat(64)
        val body = "Reviewed notes\n\nSHA-256: $hash\nAndroid-Version-Code: 155"
        val connection = Connection(200, document(body, sidecar = false).toString())
        val result = UpdateCheck.check(154) { url ->
            assertEquals(UPDATE_LATEST_URL, url)
            connection
        }
        val release = (result as UpdateCheckResult.Available).release
        assertEquals(hash, release.sha256)
        assertEquals("", release.checksumUrl)
        assertTrue(connection.closed)
    }

    @Test fun checkOffersSidecarFallbackRelease() = runBlocking {
        val connection = Connection(200, document().toString())
        val result = UpdateCheck.check(154) { connection }
        val release = (result as UpdateCheckResult.Available).release
        assertEquals("", release.sha256)
        assertEquals("$TEST_RELEASE_DOWNLOAD/$UPDATE_CHECKSUM", release.checksumUrl)
    }

    @Test fun checkRejectsMalformedNotesHash() = runBlocking {
        val hash = "a".repeat(64)
        val body = "Reviewed notes\n\nSHA-256: ${hash.uppercase()}\nAndroid-Version-Code: 155"
        val connection = Connection(200, document(body).toString())
        val result = UpdateCheck.check(154) { connection }
        assertTrue((result as UpdateCheckResult.Error).message.contains("malformed"))
    }
}
