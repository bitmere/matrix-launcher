package com.matrix.launcher

/** Portable personal_home settings only. Internal recovery/permission/device state stays local. */
internal object BackupSchema {
    private val booleans = setOf("safe_mode", "home_search_enabled", "simkl_home_enabled", "search_catalog_enabled",
        "search_simkl_enabled", "search_tmdb_enabled", "ai_web_search", "ai_discovery_enabled", "commons_enabled",
        "matrix_home_redirect", "matrix_home_key_filter", "matrix_gamepad_passthrough")
    private val sets = setOf("sources", "hidden_rows", "simkl_pins", "simkl_sections")
    private val strings = setOf("favorites", "row_order", "search_recent_queries", "last_search_provider",
        "ai_provider", "ai_base_url", "media_title_app", "wallpaper_mode")
    private val integers = mapOf("list_columns" to 3..6, "idle_clock_minutes" to 0..1440,
        "ai_result_count" to 5..100, "rotation_minutes" to 1..10080)
    fun type(key: String): String? = when {
        key in booleans -> "b"
        key in sets -> "ss"
        key in strings || key.startsWith("ai_model_") || key.startsWith("item_order:") -> "s"
        key in integers || key.startsWith("row_limit:") -> "i"
        else -> null
    }
    fun validate(key: String, value: Any) {
        require(key.length <= 512 && key.isNotBlank()) { "Invalid setting key" }
        val expected = type(key) ?: error("Unsupported setting: $key")
        require(when (expected) {
            "b" -> value is Boolean
            "s" -> value is String && value.length <= 32768
            "ss" -> value is Set<*> && value.size <= 512 && value.all { it is String && it.length <= 512 }
            "i" -> value is Int && value in (integers[key] ?: 1..100)
            else -> false
        }) { "Invalid value for $key" }
    }
}
