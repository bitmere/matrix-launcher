package com.matrix.launcher

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInstaller
import androidx.core.content.edit
import java.io.File

internal interface SessionStore {
    fun sessionExists(id: Int): Boolean
    fun abandonSession(id: Int)
}

internal class PackageInstallerSessionStore(private val installer: PackageInstaller) : SessionStore {
    override fun sessionExists(id: Int) = installer.getSessionInfo(id) != null
    override fun abandonSession(id: Int) = installer.abandonSession(id)
}

internal object PendingUpdateSession {
    private fun prefs(context: Context) = context.getSharedPreferences("matrix_update_session", Context.MODE_PRIVATE)
    fun save(context: Context, id: Int, version: Long, file: File) {
        prefs(context).edit(commit = true) {
            putInt("id", id)
            putLong("version", version)
            putString("path", file.absolutePath)
        }
    }
    fun matches(context: Context, id: Int, version: Long, path: String?): Boolean {
        val state = prefs(context)
        return id >= 0 && state.getInt("id", -1) == id && state.getLong("version", -1) == version && state.getString("path", null) == path
    }
    fun savedId(context: Context): Int = prefs(context).getInt("id", -1)

    fun clear(context: Context, id: Int) = clear(prefs(context), id)

    private fun clear(state: SharedPreferences, id: Int) {
        if (state.getInt("id", -1) == id) state.edit(commit = true) { clear() }
    }

    fun settle(store: SessionStore, id: Int): Boolean = try {
        settleOrThrow(store, id)
        true
    } catch (_: Exception) { false }

    private fun settleOrThrow(store: SessionStore, id: Int) {
        fun gone(): Boolean = try { !store.sessionExists(id) } catch (_: Exception) { false }
        if (gone()) return
        try {
            store.abandonSession(id)
        } catch (failure: SecurityException) {
            // The session may disappear between the existence check and abandonment.
            if (!gone()) throw failure
        }
    }

    fun abandon(context: Context, store: SessionStore = PackageInstallerSessionStore(context.packageManager.packageInstaller)) =
        abandon(prefs(context), store)

    internal fun abandon(state: SharedPreferences, store: SessionStore) {
        val id = state.getInt("id", -1)
        if (id >= 0) {
            // Never replace a pending operation unless its session has been settled.
            settleOrThrow(store, id)
            clear(state, id)
        }
    }
}
