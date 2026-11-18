package com.matrix.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class SimklListChoice(val id: String, val title: String, val count: Int)

/** One client and lock shared with Search: auth, quota and cache state cannot diverge.
 * The SIMKL lock (D-03) is separate from Home's lock — a sign-in poll or list
 * read must not stall Home load. Lock order stays home -> SIMKL -> client monitor. */
internal class MatrixAccountsRepository(private val client: SimklClient, private val simklGate: Mutex) {
    val connected get() = client.connected()
    val name get() = client.name()
    val clientId get() = client.clientId()
    suspend fun saveClientId(value: String) = withContext(Dispatchers.IO) { simklGate.withLock { client.clientId(value.trim()) } }
    fun usage() = client.usageSummary()
    suspend fun begin(): SimklClient.Pin = withContext(Dispatchers.IO) { simklGate.withLock { client.begin() } }
    suspend fun poll(pin: SimklClient.Pin, active: () -> Boolean): Boolean = withContext(Dispatchers.IO) {
        simklGate.withLock { client.poll(pin, active) }
    }
    suspend fun disconnect() = withContext(Dispatchers.IO) { simklGate.withLock { client.disconnect() } }
    suspend fun lists(): List<SimklListChoice> = withContext(Dispatchers.IO) { simklGate.withLock {
        val array = client.index().optJSONArray("lists") ?: return@withLock emptyList()
        List(array.length()) { index -> val item = array.getJSONObject(index)
            SimklListChoice(item.getString("id"), item.optString("name", "Untitled"), item.optJSONObject("counts")?.optInt("items") ?: 0)
        }
    } }
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) { simklGate.withLock {
        try {
            if (!client.connected()) return@withLock Result.failure(IllegalStateException("SIMKL is not connected. Connect via QR code first."))
            val userName = client.name().ifBlank { "Connected user" }
            val index = client.index()
            val listCount = index.optJSONArray("lists")?.length() ?: 0
            Result.success("Success: Connected to SIMKL as $userName ($listCount lists available)")
        } catch (e: Exception) {
            Result.failure(Exception("SIMKL test failed: ${e.message ?: e.javaClass.simpleName}"))
        }
    } }
}
