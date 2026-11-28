package com.matrix.launcher

internal enum class DocumentLaunch { STARTED, UNAVAILABLE, DENIED, BUSY }

/** Main-thread pending-result owner; null results mean cancellation, never launch failure. */
internal class PendingDocument<T> {
    private var callback: ((T?) -> Unit)? = null
    private var inFlight = false

    fun launch(onResult: (T?) -> Unit, resolve: () -> Boolean,
        missingHandler: (RuntimeException) -> Boolean, start: () -> Unit): DocumentLaunch {
        if (inFlight) return DocumentLaunch.BUSY
        callback = onResult
        val outcome = try {
            if (!resolve()) DocumentLaunch.UNAVAILABLE else {
                inFlight = true
                start()
                DocumentLaunch.STARTED
            }
        } catch (_: SecurityException) {
            DocumentLaunch.DENIED
        } catch (failure: RuntimeException) {
            if (!missingHandler(failure)) {
                callback = null
                inFlight = false
                throw failure
            }
            DocumentLaunch.UNAVAILABLE
        }
        if (outcome != DocumentLaunch.STARTED) {
            callback = null
            inFlight = false
        }
        return outcome
    }

    fun complete(value: T?) {
        val result = callback
        callback = null
        inFlight = false
        result?.invoke(value)
    }

    // Keep the flight reserved until its result arrives, so a stale result cannot
    // be delivered to a newly opened page.
    fun cancel() { callback = null }
}
