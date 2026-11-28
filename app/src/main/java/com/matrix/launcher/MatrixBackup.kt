package com.matrix.launcher

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings backup export/import as versioned JSON, via the Storage Access
 * Framework (no new permissions).
 *
 * Format: `{"format":"matrix-backup","version":1,"prefs":{...},"theme":{...}|null}`.
 * Each pref entry is a typed wrapper so a round-trip restores the exact type:
 * `{"t":"s","v":"..."}` (String), `"i"` (Int), `"l"` (Long), `"b"` (Boolean),
 * `"ss"` (String set, JSON array). Secrets are never exported and never
 * imported, even if a hand-edited file contains one. The SIMKL account token
 * lives in the separate `simkl_v2` prefs and is out of scope; the user
 * re-links SIMKL after a reinstall.
 *
 * The codec ([encode]/[decode]) is pure (String in/out, no Context) so it is
 * JVM-testable. Transport lives in the settings pages through [MatrixSaf].
 */
internal class BackupCommitException(cause: Throwable? = null) :
    java.io.IOException("Settings restore failed; theme may already be restored", cause)

internal data class MatrixBackupData(val prefs: Map<String, Any>, val themeJson: String?)

internal object MatrixBackup {
    const val MAX_BYTES = 512 * 1024
    const val FORMAT = "matrix-backup"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"

    /** A key is a secret when it starts with `ai_api_key`, or contains `token`, `password`, or `secret`. */
    fun isSecretKey(key: String): Boolean {
        val lower = key.lowercase(Locale.US)
        return lower == MatrixPrefs.TMDB_API_KEY || lower.startsWith(MatrixPrefs.AI_API_KEY_PREFIX) ||
            lower.startsWith(MatrixPrefs.AI_API_KEY_LEGACY) ||
            "token" in lower || "password" in lower || "secret" in lower
    }

    fun defaultFileName(date: Date = Date()): String =
        "matrix-backup-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(date) + ".json"

    fun encode(prefs: Map<String, *>, themeJson: String?): String {
        val prefsJson = JSONObject()
        for ((key, value) in prefs) {
            if (isSecretKey(key) || BackupSchema.type(key) == null) continue
            if (value != null) BackupSchema.validate(key, value)
            val entry = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Set<*> -> JSONObject().put("t", "ss")
                    .put("v", JSONArray(value.filterIsInstance<String>()))
                else -> null
            } ?: continue
            prefsJson.put(key, entry)
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("prefs", prefsJson)
            .put("theme", themeJson?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject.NULL)
            .toString()
    }

    fun decode(json: String): Result<MatrixBackupData> = runCatching {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds size limit" }
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "Not a Matrix backup (format must be $FORMAT)" }
        require(root.optInt("version", -1) == VERSION) { "Unsupported backup version ${root.optInt("version", -1)} (expected $VERSION)" }
        val prefsJson = root.optJSONObject("prefs")
            ?: throw IllegalArgumentException("Backup has no prefs object")
        require(prefsJson.length() <= 1024) { "Too many settings" }
        val prefs = mutableMapOf<String, Any>()
        for (key in prefsJson.keys()) {
            if (isSecretKey(key)) continue
            // Ignore known local-only bookkeeping from older exports; reject unknown settings.
            if (key in setOf("safe_mode_crashes", "listings_permission_requested", "probe_boot",
                    "commons_attempt", "commons_status", "commons_offset", "wallpaper_current", "wallpaper_changed",
                    "wallpaper_reddit_removed", "reddit_client", "reddit_refresh", "reddit_enabled",
                    "reddit_attempt", "subreddits")) continue
            val expected = BackupSchema.type(key) ?: error("Unsupported setting: $key")
            val entry = prefsJson.getJSONObject(key)
            require(entry.get("t") == expected) { "Wrong setting type: $key" }
            val raw = entry.get("v")
            val value: Any = when (expected) {
                "s" -> raw as? String ?: error("Expected text")
                "b" -> raw as? Boolean ?: error("Expected boolean")
                "i" -> {
                    require(raw is Number && raw.toString().matches(Regex("-?[0-9]+")))
                    raw.toString().toInt()
                }
                "ss" -> {
                    val array = raw as? JSONArray ?: error("Expected set")
                    require(array.length() <= 512) { "Too many set entries" }
                    (0 until array.length()).map { array.get(it) as? String ?: error("Expected set text") }.toSet()
                }
                else -> error("Unsupported type")
            }
            BackupSchema.validate(key, value)
            prefs[key] = value
        }
        val themeObj = root.optJSONObject("theme")
        val themeJson = when {
            root.isNull("theme") -> null
            themeObj != null -> themeObj.toString()
            else -> root.optString("theme").takeIf { it.isNotBlank() }
        }
        themeJson?.let { require(MatrixTheme.parse(it).ignoredReason == null) { "Invalid backup theme" } }
        MatrixBackupData(prefs, themeJson)
    }

    /** Non-secret entries of the personal_home prefs, for export.
     * Null-valued entries are dropped — `encode` would skip them anyway. */
    fun exportPrefs(prefs: SharedPreferences): Map<String, Any> =
        prefs.all.entries
            .filter { !isSecretKey(it.key) && BackupSchema.type(it.key) != null }
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap()

    /** theme.json contents when present, for export. */
    fun readThemeJson(filesDir: File): String? {
        val file = File(filesDir, THEME_FILE_NAME)
        return if (file.exists()) runCatching { file.inputStream().use { BoundedInput.readText(it, MatrixTheme.MAX_BYTES) } }.getOrNull() else null
    }

    /**
     * Write an imported backup: prefs (never a secret key), then theme.json
     * when present. Returns how many settings were restored.
     */
    fun apply(context: Context, prefs: SharedPreferences, data: MatrixBackupData): Int =
        apply(context.filesDir, prefs, data)

    @android.annotation.SuppressLint("ApplySharedPref") // Runs on IO; commit() is required to detect restore failure.
    internal fun apply(filesDir: File, prefs: SharedPreferences, data: MatrixBackupData): Int {
        val portable = data.prefs.filterKeys { !isSecretKey(it) }
        require(portable.size <= 1024)
        portable.forEach { (key, value) -> BackupSchema.validate(key, value) }
        // Persist theme first. A failed preference commit reports partial failure explicitly.
        data.themeJson?.let { theme ->
            MatrixTheme.import(filesDir, theme.byteInputStream())
        }
        val editor = prefs.edit()
        with(editor) {
            for ((key, value) in portable) {
                if (isSecretKey(key)) continue
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }
        val committed = try { editor.commit() } catch (failure: Exception) { throw BackupCommitException(failure) }
        if (!committed) throw BackupCommitException()
        return portable.size
    }
}

/**
 * Storage Access Framework launchers, registered by ComposeHomeActivity in
 * onCreate and consumed by the settings pages through the settings scope.
 * The scope fields default to these (null until the activity registers
 * them), so MatrixHomeScreen.kt needs no change to wire them.
 */
internal object MatrixSaf {
    fun open(types: Array<String>, result: (Uri?) -> Unit): DocumentLaunch =
        openDocument?.invoke(types, result) ?: DocumentLaunch.UNAVAILABLE

    fun create(name: String, mime: String, result: (Uri?) -> Unit): DocumentLaunch =
        createDocument?.invoke(name, mime, result) ?: DocumentLaunch.UNAVAILABLE

    fun notice(context: Context, outcome: DocumentLaunch, show: (String) -> Unit) {
        val resource = when (outcome) {
            DocumentLaunch.STARTED -> return
            DocumentLaunch.UNAVAILABLE -> R.string.document_picker_unavailable
            DocumentLaunch.DENIED -> R.string.document_picker_denied
            DocumentLaunch.BUSY -> R.string.document_picker_busy
        }
        show(context.getString(resource))
    }

    fun release(activity: Any) {
        if (owner !== activity) return
        cancelPending?.invoke()
        openDocument = null
        createDocument = null
        cancelPending = null
        owner = null
    }

    var owner: Any? = null
    var cancelPending: (() -> Unit)? = null
    var openDocument: ((mimeTypes: Array<String>, onResult: (Uri?) -> Unit) -> DocumentLaunch)? = null
    var createDocument: ((fileName: String, mimeType: String, onResult: (Uri?) -> Unit) -> DocumentLaunch)? = null
}
