package com.matrix.launcher

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.InputStream

class AuditRegressionTest {
    @Test fun safDestructionCannotClearReplacementActivity() {
        val old = Any()
        val current = Any()
        var cancelled = false
        MatrixSaf.owner = current
        MatrixSaf.cancelPending = { cancelled = true }
        MatrixSaf.openDocument = { _, _ -> DocumentLaunch.STARTED }
        MatrixSaf.release(old)
        assertFalse(cancelled)
        assertNotNull(MatrixSaf.openDocument)
        MatrixSaf.release(current)
        assertTrue(cancelled)
        assertNull(MatrixSaf.owner)
        assertNull(MatrixSaf.openDocument)
        assertNull(MatrixSaf.cancelPending)
    }

    @Test fun intentOnlySafeModeDisablesAiWithoutChangingStoredPreference() {
        val prefs = mapBackedPrefs(mutableMapOf("ai_discovery_enabled" to true))
        val effective = MatrixSafeMode.onStartup(prefs, true)
        val ai = MatrixAiClient(prefs).also { it.safeMode = { effective } }
        assertTrue(effective)
        assertFalse(MatrixSafeMode.isOn(prefs))
        assertFalse(ai.enabled)
    }

    @Test fun failedPreferenceCommitReportsPossiblePartialRestore() {
        val dir = Files.createTempDirectory("restore-failure").toFile()
        try {
            val result = runCatching { MatrixBackup.apply(dir, mapBackedPrefs(commitSucceeds = false),
                MatrixBackupData(mapOf("safe_mode" to true), MatrixTheme.encode(ThemeState.defaults()))) }
            assertTrue(result.exceptionOrNull() is BackupCommitException)
            assertTrue(java.io.File(dir, THEME_FILE_NAME).exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun applyValidatesEveryEntryBeforeTouchingTypedPreferencesOrTheme() {
        val dir = Files.createTempDirectory("restore-atomic").toFile()
        val values = mutableMapOf<String, Any>("safe_mode" to false, "list_columns" to 5)
        val prefs = mapBackedPrefs(values)
        try {
            MatrixTheme.save(dir, ThemeState.defaults())
            val original = java.io.File(dir, THEME_FILE_NAME).readText()
            val invalid = MatrixBackupData(linkedMapOf("list_columns" to 4, "safe_mode" to "false"), null)
            assertTrue(runCatching { MatrixBackup.apply(dir, prefs, invalid) }.isFailure)
            assertFalse(prefs.getBoolean("safe_mode", true))
            assertEquals(5, prefs.getInt("list_columns", 0))
            assertEquals(original, java.io.File(dir, THEME_FILE_NAME).readText())
            assertEquals(2, MatrixBackup.apply(dir, prefs, MatrixBackupData(mapOf("safe_mode" to true, "list_columns" to 4), null)))
            assertTrue(prefs.getBoolean("safe_mode", false))
            assertEquals(4, prefs.getInt("list_columns", 0))
        } finally { dir.deleteRecursively() }
    }

    @Test fun lateHighRankedChannelItemReplacesWorstRetainedItem() {
        val row = TvFeed.Row("fixture")
        repeat(12) { index -> TvFeed.best(row, TvFeed.Item().apply { weight = index.toLong() }) }
        val winner = TvFeed.Item().apply { weight = 1000 }
        TvFeed.best(row, winner)
        assertEquals(12, row.items.size)
        assertTrue(row.items.contains(winner))
        assertFalse(row.items.any { it.weight == 0L })
    }

    @Test fun wrongTypesAndRangesRejectEntireBackup() {
        val fixtures = listOf("safe_mode" to "false", "idle_clock_minutes" to "5", "sources" to "[]", "list_columns" to true)
        for ((key, value) in fixtures) {
            val entry = JSONObject().put("t", if (value is String) "s" else "b").put("v", value)
            val doc = JSONObject(MatrixBackup.encode(mapOf("favorites" to "[]"), null))
            doc.getJSONObject("prefs").put(key, entry)
            assertTrue(key, MatrixBackup.decode(doc.toString()).isFailure)
        }
        val doc = JSONObject(MatrixBackup.encode(mapOf("list_columns" to 5), null))
        doc.getJSONObject("prefs").getJSONObject("list_columns").put("v", 999)
        assertTrue(MatrixBackup.decode(doc.toString()).isFailure)
        val prefs = mapBackedPrefs(mutableMapOf("safe_mode" to "false"))
        try { prefs.getBoolean("safe_mode", false); fail("Fake must throw like Android") } catch (_: ClassCastException) { }
    }

    @Test fun internalStateAndSecretsNeverTravel() {
        val encoded = MatrixBackup.encode(mapOf("safe_mode_crashes" to 3, "listings_permission_requested" to true,
            "tmdb_api_key" to "fixture-credential", "safe_mode" to true), null)
        assertEquals(mapOf("safe_mode" to true), MatrixBackup.decode(encoded).getOrThrow().prefs)
    }

    @Test fun collectionAndDocumentBoundsRejectBeforeApply() {
        val doc = JSONObject(MatrixBackup.encode(emptyMap<String, Any>(), null))
        doc.getJSONObject("prefs").put("sources", JSONObject().put("t", "ss").put("v", org.json.JSONArray(List(513) { "pkg.$it" })))
        assertTrue(MatrixBackup.decode(doc.toString()).isFailure)
        assertTrue(MatrixBackup.decode(" ".repeat(MatrixBackup.MAX_BYTES + 1)).isFailure)
        for (limit in listOf(MatrixTheme.MAX_BYTES, MatrixBackup.MAX_BYTES, 1024 * 1024)) {
            try { BoundedInput.readText(ByteArray(limit + 1).inputStream(), limit); fail("Accepted excess bytes") } catch (_: java.io.IOException) { }
        }
    }

    @Test fun failedImportsKeepKnownGoodTheme() {
        val dir = Files.createTempDirectory("theme-regression").toFile()
        try {
            MatrixTheme.save(dir, ThemeState.defaults())
            val file = java.io.File(dir, THEME_FILE_NAME)
            val original = file.readText()
            for (input in listOf("{invalid".byteInputStream(), ByteArray(MatrixTheme.MAX_BYTES + 1).inputStream(), object : InputStream() {
                override fun read(): Int = throw java.io.IOException("Interrupted")
            })) {
                assertTrue(runCatching { input.use { MatrixTheme.import(dir, it) } }.isFailure)
                assertEquals(original, file.readText())
            }
            file.writeBytes(ByteArray(MatrixTheme.MAX_BYTES + 1))
            assertNotNull(MatrixTheme.load(dir).ignoredReason)
        } finally { dir.deleteRecursively() }
    }

    @Test fun titleIdentityAndMediaKindSurviveResolution() {
        assertNotEquals(cleanTitle("千と千尋"), cleanTitle("となりのトトロ"))
        val rec = AiRecommendation("Title", "2024", "movie", "", "")
        val series = MediaCard("s", "Title", "", "", metadata = "2024", mediaType = "Series")
        val movie = series.copy(id = "m", mediaType = "Movie")
        assertEquals(movie, matchRecommendation(rec, listOf(series, movie)))
        assertNull(matchRecommendation(rec, listOf(movie.copy(metadata = "1990"))))
    }

    @Test fun removalDeletesLegacyAndAllProviderModelsOnly() {
        val map = mutableMapOf<String, Any>("ai_api_key" to "fixture", "ai_api_key_DEEPSEEK" to "fixture",
            "ai_api_key_DEEPSEEK_model1" to "fixture", "ai_api_key_DEEPSEEK_model2" to "fixture", "ai_api_key_GEMINI" to "fixture")
        val ai = MatrixAiClient(mapBackedPrefs(map))
        ai.provider = MatrixAiProvider.DEEPSEEK
        ai.removeApiKey()
        assertTrue(ai.apiKey.isEmpty())
        assertFalse(map.keys.any { it == "ai_api_key" || it.startsWith("ai_api_key_DEEPSEEK") })
        assertTrue(map.containsKey("ai_api_key_GEMINI"))
    }

    @Test fun obsoleteProgressAndResultsAreRejectedAfterClearOrReplacement() {
        val operation = SearchOperation()
        val old = operation.begin()
        operation.invalidate()
        assertFalse(operation.isCurrent(old))
        val replacement = operation.begin()
        assertTrue(operation.isCurrent(replacement))
        assertFalse(operation.isCurrent(old))
    }

    @Test fun webCacheSeparatesThreeAndEightInBothOrders() {
        val cache = mutableMapOf<String, List<Int>>()
        for (order in listOf(listOf(3, 8), listOf(8, 3))) {
            cache.clear()
            order.forEach { count -> cache[webSearchCacheKey(" Query ", count)] = (1..count).toList() }
            assertEquals(3, cache.getValue(webSearchCacheKey("query", 3)).size)
            assertEquals(8, cache.getValue(webSearchCacheKey("QUERY", 8)).size)
        }
    }

    @Test fun sameProcessRadiusChangesRefreshShape() {
        val original = MatrixLayout.theme
        try {
            MatrixTheme.apply(original.copy(radiusDp = 0f)); val first = MatrixCardDefaults.artworkShape
            MatrixTheme.apply(original.copy(radiusDp = 32f))
            assertNotEquals(first, MatrixCardDefaults.artworkShape)
        } finally { MatrixTheme.apply(original) }
    }
}
