package com.matrix.launcher

import android.content.Context
import androidx.core.content.edit
import java.io.File

internal object PendingUpdateSession {
    private fun prefs(context: Context) = context.getSharedPreferences("matrix_update_session", Context.MODE_PRIVATE)
    fun save(context: Context, id: Int, version: Long, file: File) {
        check(prefs(context).edit().putInt("id", id).putLong("version", version).putString("path", file.absolutePath).commit())
    }
    fun matches(context: Context, id: Int, version: Long, path: String?): Boolean {
        val state = prefs(context)
        return id >= 0 && state.getInt("id", -1) == id && state.getLong("version", -1) == version && state.getString("path", null) == path
    }
    fun clear(context: Context, id: Int) {
        val state = prefs(context)
        if (state.getInt("id", -1) == id) state.edit { clear() }
    }
    fun abandon(context: Context) {
        val state = prefs(context)
        val id = state.getInt("id", -1)
        if (id >= 0) {
            // Never replace a pending operation unless its session has been settled.
            context.packageManager.packageInstaller.abandonSession(id)
            clear(context, id)
        }
    }
}
