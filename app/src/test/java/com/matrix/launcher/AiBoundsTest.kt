package com.matrix.launcher

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class AiBoundsTest {
    @Test fun recommendationsAreLocallyCapped() {
        val items = org.json.JSONArray()
        repeat(120) { items.put(JSONObject().put("title", "Title $it").put("year", "2024").put("type", "movie")) }
        val result = MatrixAiClient(mapBackedPrefs()).parseRecommendations(items.toString())
        assertEquals(100, result.size)
    }

    @Test fun successAndErrorBodiesAreBoundedAndDisconnected() {
        val ai = MatrixAiClient(mapBackedPrefs())
        for (status in listOf(200, 500)) {
            var disconnected = false
            val connection = object : HttpURLConnection(URL("https://example.invalid")) {
                override fun connect() { }
                override fun disconnect() { disconnected = true }
                override fun usingProxy() = false
                override fun getResponseCode() = status
                override fun getOutputStream() = ByteArrayOutputStream()
                override fun getInputStream() = ByteArray(1024 * 1024 + 1).inputStream()
                override fun getErrorStream() = inputStream
            }
            assertTrue(runCatching { ai.postJson("https://example.invalid", "", JSONObject()) { connection } }.exceptionOrNull() is java.io.IOException)
            assertTrue(disconnected)
        }
    }

    @Test fun toolsHaveOneActionOwnerAndBoundedFanOut() = runBlocking {
        val ai = MatrixAiClient(mapBackedPrefs())
        val actions = mutableListOf<String>()
        val ctx = AiDiscoverContext("fixture", "model", "", 12, 100, 2026, false, null, false,
            "", "", "", actions, null, false)
        val calls = List(16) { Triple("$it", "tv_context", JSONObject()) }
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val results = ai.executeOpenAiMcpTools(calls, ctx) { _, _ ->
            val count = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, count) }
            if (count == 4) gate.complete(Unit)
            gate.await()
            yield()
            active.decrementAndGet()
            "ok"
        }
        assertEquals(16, actions.size)
        assertEquals(calls.map { it.first }, results.map { it.first })
        assertTrue(peak.get() <= 4)
        assertTrue(runCatching { ai.executeOpenAiMcpTools(calls + calls.first(), ctx) { _, _ -> error("Must reject before execution") } }.isFailure)
        assertEquals(16, actions.size)
    }
}
