package com.matrix.launcher

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

class UpdateFlowTest {
    private val release = LatestRelease("2.7.6", 155, "$TEST_RELEASE_DOWNLOAD/$UPDATE_APK", 3,
        "$TEST_RELEASE_DOWNLOAD/$UPDATE_CHECKSUM", "")
    private val apkHash = java.security.MessageDigest.getInstance("SHA-256")
        .digest("apk".toByteArray()).joinToString("") { "%02x".format(it) }

    private fun checksum(hash: String = apkHash) =
        Connection(bytes = "$hash  $UPDATE_APK\n".toByteArray())

    private fun downloadConnections(apk: Connection, sidecar: Connection = checksum()): (String) -> Connection =
        { url -> if (url == release.checksumUrl) sidecar else apk }
    private class Connection(private val code: Int = 200, private val bytes: ByteArray = "apk".toByteArray(),
        private val location: String? = null, val body: java.io.InputStream? = null) : HttpURLConnection(URL("https://example.invalid")) {
        var closed = false
        override fun connect() { }
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getResponseCode() = code
        override fun getInputStream() = body ?: ByteArrayInputStream(bytes)
        override fun getContentLengthLong() = -1L
        override fun getHeaderField(name: String?) = location
    }

    @Test fun downloadsWithoutLengthAndDisconnects() = runBlocking {
        val dir = Files.createTempDirectory("download-test").toFile()
        try {
            val connection = Connection()
            val sidecar = checksum()
            val file = ApkDownload.download(dir, release, connect = downloadConnections(connection, sidecar)) {
                _, total -> assertEquals(3L, total ?: -1L)
            }
            assertEquals("apk", file.readText())
            assertTrue(connection.closed)
            assertTrue(sidecar.closed)
        } finally { dir.deleteRecursively() }
    }

    @Test fun networkHashAndSizeFailuresCleanOwnedFileAndAllowRetry() = runBlocking {
        val dir = Files.createTempDirectory("download-failures").toFile()
        try {
            val failedRead = object : java.io.InputStream() { override fun read(): Int = throw IOException("Read failed") }
            val failures = listOf(
                Connection(503) to IOException::class.java,
                Connection(302, location = "https://example.invalid/next") to IOException::class.java,
                Connection(body = failedRead) to IOException::class.java,
                Connection(bytes = ByteArray(2)) to IOException::class.java,
                Connection(bytes = ByteArray(4)) to IOException::class.java,
                Connection(bytes = "bad".toByteArray()) to UpdateVerificationException::class.java
            )
            for ((connection, exception) in failures) {
                val result = runCatching {
                    ApkDownload.download(dir, release, connect = downloadConnections(connection)) { _, _ -> }
                }
                assertTrue(exception.isInstance(result.exceptionOrNull()))
                assertTrue(ApkDownload.updatesDir(dir).listFiles().orEmpty().isEmpty())
                assertTrue(connection.closed)
            }
            val malformed = Connection(bytes = "invalid".toByteArray())
            val result = runCatching {
                ApkDownload.download(dir, release, connect = { url ->
                    assertEquals(release.checksumUrl, url)
                    malformed
                }) { _, _ -> }
            }
            assertTrue(result.exceptionOrNull() is UpdateVerificationException)
            assertTrue(malformed.closed)
            assertTrue(ApkDownload.download(dir, release, connect = downloadConnections(Connection())) { _, _ -> }.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun cancellationAfterFinalByteBeforeHashDeletesFile() = runBlocking {
        val dir = Files.createTempDirectory("download-cancel").toFile()
        try {
            val attempt = launch(start = CoroutineStart.LAZY) {
                val owner = currentCoroutineContext().job
                val stream = object : ByteArrayInputStream("apk".toByteArray()) {
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        val result = super.read(buffer, offset, length)
                        if (result == -1) owner.cancel()
                        return result
                    }
                }
                ApkDownload.download(dir, release, connect = downloadConnections(Connection(body = stream))) { _, _ -> }
            }
            attempt.start(); attempt.join()
            assertTrue(attempt.isCancelled)
            assertTrue(ApkDownload.updatesDir(dir).listFiles().orEmpty().none { it.extension == "apk" })
        } finally { dir.deleteRecursively() }
    }

    @Test fun hashReaderChecksCancellationBetweenChunksAndAtEnd() {
        val file = Files.createTempFile("hash", ".apk").toFile()
        try {
            file.writeBytes(ByteArray(20000))
            var checks = 0
            assertTrue(runCatching { ApkDownload.sha256Hex(file) { if (++checks == 2) throw CancellationException() } }.exceptionOrNull() is CancellationException)
            assertEquals(2, checks)
        } finally { file.delete() }
    }

    @Test fun sessionFailuresAlwaysAbandonCreatedIdAndCleanupCannotHideFailure() {
        for (stage in listOf("open", "write", "commit")) {
            val calls = mutableListOf<String>()
            val adapter = object : InstallSessionAdapter {
                override fun create(): Int { calls += "create"; return 7 }
                override fun open(id: Int) { calls += "open"; if (stage == "open") error(stage) }
                override fun writeAndSync() { calls += "write"; if (stage == "write") error(stage) }
                override fun commit(id: Int) { calls += "commit"; if (stage == "commit") error(stage) }
                override fun abandon(id: Int) { assertEquals(7, id); calls += "abandon"; error("cleanup") }
                override fun close() { calls += "close"; error("close") }
            }
            assertEquals(stage, runCatching { commitInstallSession(adapter) }.exceptionOrNull()?.message)
            assertEquals(listOf("abandon", "close"), calls.takeLast(2))
        }
    }

    @Test fun callbacksIgnoreOldAttemptAndUnregisterOnTerminalOutcome() {
        val received = mutableListOf<UpdateInstallEvents.InstallResult>()
        UpdateInstallEvents.setListener("old") { fail("Stale listener") }
        UpdateInstallEvents.setListener("new") { received += it }
        UpdateInstallEvents.publish("old", UpdateInstallEvents.InstallResult.Failed("old"))
        UpdateInstallEvents.clear("old")
        UpdateInstallEvents.publish("new", UpdateInstallEvents.InstallResult.WaitingForConfirmation)
        UpdateInstallEvents.publish("new", UpdateInstallEvents.InstallResult.Installed)
        UpdateInstallEvents.publish("new", UpdateInstallEvents.InstallResult.Failed("late"))
        assertEquals(listOf(UpdateInstallEvents.InstallResult.WaitingForConfirmation, UpdateInstallEvents.InstallResult.Installed), received)
    }

    @Test fun successfulReplacementReopensWithoutOldProcessListener() {
        UpdateInstallEvents.setListener(null, null)
        var reopened = false
        finishSuccessfulUpdate(
            notify = { UpdateInstallEvents.publish("dead-process", UpdateInstallEvents.InstallResult.Installed) },
            reopen = { reopened = true }
        )
        assertTrue(reopened)
    }

    @Test fun oldUiFailureCannotPreventReopeningAfterReplacement() {
        var reopened = false
        val failure = runCatching {
            finishSuccessfulUpdate(notify = { error("Disposed UI") }, reopen = { reopened = true })
        }.exceptionOrNull()
        assertEquals("Disposed UI", failure?.message)
        assertTrue(reopened)
    }

    private class Backend(val file: File, val release: LatestRelease) : UpdateBackend {
        var granted = false
        var settingsAvailable = false
        var downloads = 0
        var installs = 0
        override val installedName = "fixture"
        override fun canInstall() = granted
        override fun openSettings() = settingsAvailable
        override suspend fun check() = UpdateCheckResult.Available(release)
        override suspend fun download(release: LatestRelease, progress: (Long, Long?) -> Unit): File { downloads++; return file }
        override fun install(file: File, release: LatestRelease, operation: String): InstallOutcome {
            assertEquals(this.file, file)
            assertEquals(this.release, release)
            installs++
            return InstallOutcome.Error("Installer unavailable")
        }
    }
    private suspend fun settle(scope: CoroutineScope) { repeat(3) { scope.coroutineContext.job.children.toList().joinAll() } }

    @Test fun controllerPermissionDenialGrantReturnAndInstallRetryKeepVerifiedFile() = runBlocking {
        val file = Files.createTempFile("verified", ".apk").toFile()
        val scope = CoroutineScope(coroutineContext + Job())
        val backend = Backend(file, release)
        val controller = UpdateCheckController(null, scope, backend)
        try {
            controller.onRowAction(); settle(scope)
            assertTrue(controller.uiState is UpdateUiState.Available)
            controller.onRowAction(); settle(scope)
            controller.onRowAction()
            assertTrue(controller.uiState is UpdateUiState.NeedsUnknownSources)
            controller.onResume()
            assertTrue(controller.uiState is UpdateUiState.NeedsUnknownSources)
            controller.openUnknownSourcesSettings()
            assertTrue(controller.uiState is UpdateUiState.Error)
            controller.onRowAction()
            controller.onRowAction()
            backend.granted = true
            controller.onResume()
            assertTrue(controller.uiState is UpdateUiState.ReadyToInstall)
            assertEquals(0, backend.installs)
            controller.onRowAction(); settle(scope)
            assertTrue(controller.uiState is UpdateUiState.Error)
            controller.onRowAction()
            assertTrue(controller.uiState is UpdateUiState.ReadyToInstall)
            assertEquals(1, backend.downloads)
            controller.onRowAction(); settle(scope)
            assertEquals(2, backend.installs)
            assertEquals(1, backend.downloads)
            controller.dispose()
            controller.onRowAction()
            assertEquals(2, backend.installs)
        } finally { controller.dispose(); scope.cancel(); file.delete() }
    }
}
