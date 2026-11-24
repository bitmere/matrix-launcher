package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class UpdateSettleTest {
    private class Store(
        val exists: (Int) -> Boolean,
        val failure: Exception? = null
    ) : SessionStore {
        val calls = mutableListOf<String>()
        private var checks = 0
        override fun sessionExists(id: Int): Boolean {
            assertEquals(7, id)
            calls += "info"
            return exists(checks++)
        }
        override fun abandonSession(id: Int) {
            assertEquals(7, id)
            calls += "abandon"
            failure?.let { throw it }
        }
    }

    private fun verify(settled: Boolean, calls: List<String>, factory: () -> Store) {
        val direct = factory()
        assertEquals(settled, PendingUpdateSession.settle(direct, 7))
        assertEquals(calls, direct.calls)

        val state = mutableMapOf<String, Any>("id" to 7, "version" to 123L, "path" to "/update.apk")
        val original = state.toMap()
        val store = factory()
        val failure = runCatching { PendingUpdateSession.abandon(mapBackedPrefs(state), store) }.exceptionOrNull()
        if (settled) {
            assertNull(failure)
            assertTrue(state.isEmpty())
        } else {
            assertSame(store.failure, failure)
            assertEquals(original, state)
        }
        assertEquals(calls, store.calls)
    }

    @Test fun staleSessionClearsPreferencesWithoutAbandoning() =
        verify(true, listOf("info")) { Store({ false }) }

    @Test fun liveSessionIsAbandonedBeforeClearingPreferences() =
        verify(true, listOf("info", "abandon")) { Store({ true }) }

    @Test fun sessionGoneAfterSecurityExceptionClearsPreferences() =
        verify(true, listOf("info", "abandon", "info")) {
            Store({ check -> check == 0 }, SecurityException("Gone"))
        }

    @Test fun liveSessionAfterSecurityExceptionKeepsPreferencesAndThrows() =
        verify(false, listOf("info", "abandon", "info")) {
            Store({ true }, SecurityException("Not owned"))
        }

    @Test fun otherAbandonFailureKeepsPreferencesAndThrows() =
        verify(false, listOf("info", "abandon")) {
            Store({ true }, IllegalStateException("Installer unavailable"))
        }

    @Test fun noSavedIdMakesNoStoreCalls() {
        val store = Store({ throw AssertionError("Unexpected lookup") })
        val state = mutableMapOf<String, Any>()
        PendingUpdateSession.abandon(mapBackedPrefs(state), store)
        assertTrue(store.calls.isEmpty())
        assertTrue(state.isEmpty())
    }

    @Test fun failedExistenceCheckStillAttemptsAbandon() =
        verify(true, listOf("info", "abandon")) {
            Store({ throw IllegalStateException("Lookup unavailable") })
        }

    @Test fun failedExistenceCheckAndSessionGoneOnRecheckSettles() =
        verify(true, listOf("info", "abandon", "info")) {
            Store({ check -> if (check == 0) throw IllegalStateException("Lookup unavailable") else false },
                SecurityException("Gone"))
        }

    @Test fun failedRecheckDoesNotForgetSession() =
        verify(false, listOf("info", "abandon", "info")) {
            Store({ check -> if (check == 0) true else throw IllegalStateException("Lookup unavailable") },
                SecurityException("Cannot abandon"))
        }

    @Test fun settlementDoesNotClearReplacedSavedId() {
        val state = mutableMapOf<String, Any>("id" to 7)
        val store = Store({ state["id"] = 8; false })
        PendingUpdateSession.abandon(mapBackedPrefs(state), store)
        assertEquals(8, state["id"])
        assertEquals(listOf("info"), store.calls)
    }
}
