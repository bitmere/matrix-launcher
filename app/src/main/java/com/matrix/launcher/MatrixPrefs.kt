package com.matrix.launcher

/**
 * Single source of truth for the SharedPreferences keys stored in [FILE].
 *
 * [FILE] is the legacy file name ("personal_home"). It must not be renamed without a migration
 * that copies the existing values, or users lose favorites, sources, row order, SIMKL pins and
 * AI credentials. Every value below is byte-identical to the string literal it replaces, so
 * this file changes no behaviour. No new dependency, permission or service is added.
 *
 * Keys are grouped by area. Keys marked "legacy" are only ever removed by a migration, never
 * read as live settings.
 */
internal object MatrixPrefs {
    /** Legacy SharedPreferences file name. Do not rename without migration. */
    const val FILE = "personal_home"

    // Home rows / favorites
    const val FAVORITES = "favorites"
    const val SOURCES = "sources"
    const val NOW_PLAYING_ROW = "matrix:now-playing"
    const val HIDDEN_ROWS = "hidden_rows"
    const val ROW_ORDER = "row_order"
    const val LIST_COLUMNS = "list_columns"
    const val IDLE_CLOCK_MINUTES = "idle_clock_minutes"
    const val HOME_SEARCH_ENABLED = "home_search_enabled"

    // SIMKL
    const val SIMKL_HOME_ENABLED = "simkl_home_enabled"
    const val SIMKL_PINS = "simkl_pins"
    const val SIMKL_SECTIONS = "simkl_sections"

    // Search providers (MatrixSearchProvider.preference values; AI reuses AI_DISCOVERY_ENABLED)
    const val SEARCH_CATALOG_ENABLED = "search_catalog_enabled"
    const val SEARCH_SIMKL_ENABLED = "search_simkl_enabled"
    const val SEARCH_TMDB_ENABLED = "search_tmdb_enabled"
    const val TMDB_API_KEY = "tmdb_api_key"
    const val SEARCH_RECENT_QUERIES = "search_recent_queries"
    const val LAST_SEARCH_PROVIDER = "last_search_provider"

    // AI discovery (MatrixAiClient)
    const val AI_PROVIDER = "ai_provider"
    const val AI_MODEL_PREFIX = "ai_model_"
    const val AI_API_KEY_PREFIX = "ai_api_key_"
    const val AI_API_KEY_LEGACY = "ai_api_key"
    const val AI_BASE_URL = "ai_base_url"
    const val AI_RESULT_COUNT = "ai_result_count"
    const val AI_WEB_SEARCH = "ai_web_search"
    const val AI_DISCOVERY_ENABLED = "ai_discovery_enabled"

    // Media / title opening
    const val MEDIA_TITLE_APP = "media_title_app"

    // Home control / remote
    const val MATRIX_HOME_REDIRECT = "matrix_home_redirect"
    const val MATRIX_HOME_KEY_FILTER = "matrix_home_key_filter"
    const val MATRIX_GAMEPAD_PASSTHROUGH = "matrix_gamepad_passthrough"

    // TV listings permission
    const val LISTINGS_PERMISSION_REQUESTED = "listings_permission_requested"

    // Wallpapers (MatrixWallpaperRepository / PersonalWallpapers / CommonsWallpapers)
    const val COMMONS_ENABLED = "commons_enabled"
    const val COMMONS_ATTEMPT = "commons_attempt"
    const val COMMONS_STATUS = "commons_status"
    const val COMMONS_OFFSET = "commons_offset"
    const val WALLPAPER_MODE = "wallpaper_mode"
    const val WALLPAPER_CURRENT = "wallpaper_current"
    const val WALLPAPER_CHANGED = "wallpaper_changed"
    const val ROTATION_MINUTES = "rotation_minutes"
    const val WALLPAPER_REDDIT_REMOVED = "wallpaper_reddit_removed"

    // Legacy Reddit keys: only ever removed by PersonalWallpapers.migratePreferences, never read.
    const val REDDIT_CLIENT = "reddit_client"
    const val REDDIT_REFRESH = "reddit_refresh"
    const val REDDIT_ENABLED = "reddit_enabled"
    const val REDDIT_ATTEMPT = "reddit_attempt"
    const val SUBREDDITS = "subreddits"

    // Safe mode (MatrixSafeMode): fallback Home with wallpaper, AI discovery
    // and custom rows off. SAFE_MODE_CRASHES counts consecutive starts that
    // never reached a first successful composition.
    const val SAFE_MODE = "safe_mode"
    const val SAFE_MODE_CRASHES = "safe_mode_crashes"

    // Defaults (identical to the literals previously used at each call site)
    const val LIST_COLUMNS_DEFAULT = 5
    const val ROW_LIMIT_DEFAULT = 12
    const val AI_RESULT_COUNT_DEFAULT = 12
    const val IDLE_CLOCK_DEFAULT = 0
    const val ROTATION_MINUTES_DEFAULT = 30
    const val WALLPAPER_MODE_DEFAULT = "both"

    /** Per-app card limit key, e.g. "row_limit:com.stremio.one". */
    fun rowLimit(pkg: String) = "row_limit:$pkg"

    /** Per-row card-order key, e.g. "item_order:<rowId>". */
    fun itemOrder(rowId: String) = "item_order:$rowId"

    /** Per-provider API key, e.g. "ai_api_key_DEEPSEEK". */
    fun aiApiKey(provider: String) = "ai_api_key_$provider"

    /** Per-provider, per-model API key, e.g. "ai_api_key_DEEPSEEK_deepseek-chat". */
    fun aiApiKey(provider: String, model: String) = "ai_api_key_${provider}_$model"

    /** Per-provider selected model, e.g. "ai_model_DEEPSEEK". */
    fun aiModel(provider: String) = "ai_model_$provider"
}
