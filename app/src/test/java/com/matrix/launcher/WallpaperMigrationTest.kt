package com.matrix.launcher

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class WallpaperMigrationTest {
    @Test fun removesOnlyRetiredCredentialsAndPreservesWallpaperAndAccounts() {
        val values = mutableMapOf<String, Any>("reddit_client" to "client", "reddit_refresh" to "secret",
            "reddit_enabled" to true, "reddit_attempt" to 123L, "subreddits" to "landscapes",
            "wallpaper_mode" to "reddit", "wallpaper_current" to "/wallpapers/reddit/saved.jpg",
            "rotation_minutes" to 60, "simkl_access" to "unchanged")
        val prefs = preferences(values)
        PersonalWallpapers.migratePreferences(prefs)
        assertFalse(values.containsKey("reddit_refresh"))
        assertFalse(values.containsKey("reddit_client"))
        assertFalse(values.containsKey("reddit_enabled"))
        assertFalse(values.containsKey("reddit_attempt"))
        assertFalse(values.containsKey("subreddits"))
        assertEquals("both", values["wallpaper_mode"])
        assertEquals("/wallpapers/reddit/saved.jpg", values["wallpaper_current"])
        assertEquals(60, values["rotation_minutes"])
        assertEquals("unchanged", values["simkl_access"])
        val after = values.toMap()
        PersonalWallpapers.migratePreferences(prefs)
        assertEquals(after, values)
    }
    @Test fun migrationRespectsWallpaperOffAndDoesNotEnableDownloads() {
        val values = mutableMapOf<String, Any>("wallpaper_mode" to "off")
        PersonalWallpapers.migratePreferences(preferences(values))
        assertEquals("off", values["wallpaper_mode"])
        assertFalse(values.containsKey("commons_enabled"))
    }
    private fun preferences(values: MutableMap<String, Any>): SharedPreferences {
        val pending = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "remove" -> { pending[args[0] as String] = null; proxy }
                "putBoolean", "putString" -> { pending[args[0] as String] = args[1]; proxy }
                "apply" -> { pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }; pending.clear(); null }
                else -> error(method.name)
            }
        }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "edit" -> editor
                "getBoolean", "getString" -> values[args[0]] ?: args[1]
                else -> error(method.name)
            }
        } as SharedPreferences
    }
}
