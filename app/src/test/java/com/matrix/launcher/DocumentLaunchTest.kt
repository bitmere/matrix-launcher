package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

/** No Android runtime: the adapter maps ActivityNotFoundException to MissingHandler here. */
class DocumentLaunchTest {
    private class MissingHandler : RuntimeException()
    private val pending = PendingDocument<String>()
    private val results = mutableListOf<String?>()
    private fun launch(resolve: () -> Boolean = { true }, start: () -> Unit = {}) =
        pending.launch({ results.add(it) }, resolve, { it is MissingHandler }, start)

    @Test fun missingHandlerDoesNotLaunchOrDeliverCancellation() {
        assertEquals(DocumentLaunch.UNAVAILABLE, launch({ false }) { fail("Launched") })
        pending.complete("stale")
        assertTrue(results.isEmpty())
    }

    @Test fun handlerVanishesAfterPreflightClearsCallback() {
        assertEquals(DocumentLaunch.UNAVAILABLE, launch { throw MissingHandler() })
        pending.complete("stale")
        assertTrue(results.isEmpty())
        assertEquals(DocumentLaunch.STARTED, launch())
    }

    @Test fun deniedLaunchClearsCallback() {
        assertEquals(DocumentLaunch.DENIED, launch { throw SecurityException("Denied") })
        pending.complete("stale")
        assertTrue(results.isEmpty())
    }

    @Test fun deniedPreflightClearsCallback() {
        assertEquals(DocumentLaunch.DENIED, launch({ throw SecurityException("Denied") }))
        pending.complete(null)
        assertTrue(results.isEmpty())
    }

    @Test fun cancellationIsDeliveredOnceAndDiffersFromFailure() {
        assertEquals(DocumentLaunch.STARTED, launch())
        pending.complete(null)
        pending.complete(null)
        assertEquals(listOf<String?>(null), results)
    }

    @Test fun pageCleanupDropsLateResultAndRejectsCompetingLaunch() {
        launch()
        pending.cancel()
        assertEquals(DocumentLaunch.BUSY, launch())
        pending.complete("old")
        assertTrue(results.isEmpty())
        assertEquals(DocumentLaunch.STARTED, launch())
        pending.complete("new")
        assertEquals(listOf("new"), results)
    }

    @Test fun callbackIsClearedBeforeReentrantLaunch() {
        pending.launch({
            assertEquals(DocumentLaunch.STARTED, launch())
        }, { true }, { false }, {})
        pending.complete("first")
        pending.complete("second")
        assertEquals(listOf("second"), results)
    }

    @Test fun programmingFailurePropagatesAndClearsCallback() {
        val failure = IllegalStateException("Bug")
        assertSame(failure, runCatching { launch { throw failure } }.exceptionOrNull())
        pending.complete(null)
        assertTrue(results.isEmpty())
        assertEquals(DocumentLaunch.STARTED, launch())
    }

    @Test fun unwiredSafReturnsUnavailableWithoutCancellation() {
        val owner = Any()
        MatrixSaf.owner = owner
        MatrixSaf.release(owner)
        assertEquals(DocumentLaunch.UNAVAILABLE, MatrixSaf.open(arrayOf("image/*")) { fail("Cancelled") })
        assertEquals(DocumentLaunch.UNAVAILABLE, MatrixSaf.create("backup.json", MatrixBackup.MIME_TYPE) { fail("Cancelled") })
    }

    @Test fun safRoundTripWithFakeHandlerPreservesBackup() {
        // Exercise both asynchronous launch/result paths with a fake document provider.
        // Actual contract intent and URI grants still require Android device validation.
        val documents = mutableMapOf<String, String>()
        val original = mapOf<String, Any>("list_columns" to 5)
        pending.launch({ uri ->
            documents[requireNotNull(uri)] = MatrixBackup.encode(original, null)
        }, { true }, { false }, {})
        pending.complete("content://test/backup.json")
        var restored: MatrixBackupData? = null
        pending.launch({ uri ->
            restored = MatrixBackup.decode(documents.getValue(requireNotNull(uri))).getOrThrow()
        }, { true }, { false }, {})
        pending.complete("content://test/backup.json")
        assertEquals(original, restored?.prefs)
    }
}
