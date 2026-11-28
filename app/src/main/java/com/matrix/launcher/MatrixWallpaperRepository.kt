package com.matrix.launcher

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Disk/network operations never run in composition or on the main thread. */
internal class MatrixWallpaperRepository(context: Context, private val prefs: SharedPreferences) {
    private val store by lazy { PersonalWallpapers(context.applicationContext, prefs) }
    companion object { private val gate = Mutex() }
    suspend fun current(next: Boolean = false) = withContext(Dispatchers.IO) { gate.withLock { store.current(next)?.absolutePath } }
    /** Polling is cheap in retained memory; byte identity detects same-path rewrites, even with equal mtime/length. */
    suspend fun contentIdentity(path: String?): String? = withContext(Dispatchers.IO) {
        if (path == null) return@withContext null
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            java.io.File(path).inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: java.io.IOException) { null }
    }
    suspend fun import(uris: List<Uri>) = withContext(Dispatchers.IO) { gate.withLock {
        val job = currentCoroutineContext()
        store.importImages(uris) { job.isActive }
    } }
    suspend fun collection(): List<java.io.File> = withContext(Dispatchers.IO) { gate.withLock { store.collection() } }
    suspend fun select(path: String): Boolean = withContext(Dispatchers.IO) { gate.withLock { store.select(path) } }
    suspend fun credits(): List<WallpaperCredit> = withContext(Dispatchers.IO) { gate.withLock { store.initialize(); CommonsWallpapers.credits(store.downloaded) } }
    suspend fun refresh(force: Boolean): String? = withContext(Dispatchers.IO) { gate.withLock {
        store.initialize()
        val now = System.currentTimeMillis()
        val elapsed = now - prefs.getLong("commons_attempt", 0)
        if (!force && (!prefs.getBoolean("commons_enabled", false) || elapsed < 24L * 60 * 60 * 1000)) return@withLock null
        if (force && elapsed < 60_000) return@withLock "Recently checked Commons. Try again in one minute; saved images still rotate offline."
        prefs.edit { putLong("commons_attempt", now) }
        val job = currentCoroutineContext()
        CommonsWallpapers.refresh(store.downloaded, prefs) { job.isActive }.also {
            prefs.edit { putString("commons_status", it) }
        }
    } }
}
