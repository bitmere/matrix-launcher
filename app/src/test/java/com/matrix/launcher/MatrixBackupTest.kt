package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

/** JVM-only tests for the pure backup codec (MatrixBackup). */
class MatrixBackupTest {

    private val prefs = mapOf<String, Any>(
        "favorites" to "[\"com.stremio.one\"]",
        "list_columns" to 5,
        "ai_discovery_enabled" to true,
        "simkl_pins" to setOf("a", "b")
    )
    private val theme = """{"background":"#FF000000","text":"#FFFFFFFF"}"""

    @Test fun encodeDecodeRoundTrip() {
        val decoded = MatrixBackup.decode(MatrixBackup.encode(prefs, theme)).getOrThrow()
        assertEquals(prefs, decoded.prefs)
        // Theme is stored as a parsed object; compare by content, not spacing.
        assertEquals(org.json.JSONObject(theme).toString(), org.json.JSONObject(decoded.themeJson).toString())
    }

    @Test fun secretsExcludedFromExport() {
        val withSecrets = prefs + mapOf(
            "ai_api_key_DEEPSEEK" to "sk-secret",
            "ai_api_key" to "sk-legacy",
            "tmdb_token" to "tok",
            "tmdb_api_key" to "fixture-credential",
            "some_password" to "hunter2",
            "my_secret" to "shh"
        )
        val decoded = MatrixBackup.decode(MatrixBackup.encode(withSecrets, null)).getOrThrow()
        assertEquals(prefs, decoded.prefs)
        assertTrue(MatrixBackup.isSecretKey("ai_api_key_DEEPSEEK"))
        assertTrue(MatrixBackup.isSecretKey("ai_api_key"))
        assertTrue(MatrixBackup.isSecretKey("refresh_token"))
        assertTrue(MatrixBackup.isSecretKey("tmdb_api_key"))
        assertFalse(MatrixBackup.isSecretKey("favorites"))
    }

    @Test fun secretInFileIsNeverImported() {
        val tampered = MatrixBackup.encode(prefs, null)
            .replace("\"prefs\":{", "\"prefs\":{\"ai_api_key_DEEPSEEK\":{\"t\":\"s\",\"v\":\"sk-stolen\"},")
        val decoded = MatrixBackup.decode(tampered).getOrThrow()
        assertFalse(decoded.prefs.containsKey("ai_api_key_DEEPSEEK"))
        assertEquals(prefs, decoded.prefs)
    }

    @Test fun badVersionRejected() {
        val wrongVersion = MatrixBackup.encode(prefs, theme).replace("\"version\":1", "\"version\":2")
        assertTrue(MatrixBackup.decode(wrongVersion).isFailure)
        val wrongFormat = MatrixBackup.encode(prefs, theme).replace("matrix-backup", "other-backup")
        assertTrue(MatrixBackup.decode(wrongFormat).isFailure)
        assertTrue(MatrixBackup.decode("{not json").isFailure)
        assertTrue(MatrixBackup.decode("{}").isFailure)
    }

    @Test fun missingThemeHandled() {
        val decoded = MatrixBackup.decode(MatrixBackup.encode(prefs, null)).getOrThrow()
        assertNull(decoded.themeJson)
        val noThemeKey = org.json.JSONObject()
            .put("format", "matrix-backup").put("version", 1)
            .put("prefs", org.json.JSONObject()).toString()
        assertNull(MatrixBackup.decode(noThemeKey).getOrThrow().themeJson)
    }

    @Test fun emptyPrefsRoundTrip() {
        val decoded = MatrixBackup.decode(MatrixBackup.encode(emptyMap<String, Any>(), null)).getOrThrow()
        assertTrue(decoded.prefs.isEmpty())
        assertNull(decoded.themeJson)
    }

    @Test fun defaultFileNameHasDate() {
        val name = MatrixBackup.defaultFileName(java.util.Date(0))
        assertTrue(name.startsWith("matrix-backup-"))
        assertTrue(name.endsWith(".json"))
    }
}
