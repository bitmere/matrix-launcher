package com.matrix.launcher

/** Breadcrumbs reflect actual entry history, not an assumed settings hierarchy. */
internal fun matrixPagePath(parents: List<String>): String =
    (listOf("Home") + parents.map(::matrixRouteLabel)).distinct().joinToString(" › ")

private fun matrixRouteLabel(route: String): String = when (route) {
    "settings" -> "Settings"
    "favorites" -> "Favorites"
    "apps" -> "All apps"
    "channels" -> "Home channels"
    "sources" -> "App channels"
    "appearance" -> "Appearance"
    "size" -> "Card size"
    "limits" -> "Cards per app"
    "idle-clock" -> "Idle clock"
    "wallpapers" -> "Wallpapers"
    "wallpaper-device" -> "Import pictures"
    "wallpaper-gallery" -> "Saved wallpapers"
    "wallpaper-source" -> "Wallpaper source"
    "wallpaper-interval" -> "Rotation interval"
    "wallpaper-credits" -> "Wallpaper credits"
    "simkl" -> "SIMKL"
    "integrations" -> "Integrations"
    "media-app" -> "Open titles with"
    "search" -> "Search"
    "catalog" -> "Catalog"
    "tmdb", "tmdb-key", "tmdb-test" -> "TMDB"
    "ai", "ai-provider", "ai-model", "ai-model-custom", "ai-key", "ai-endpoint", "ai-models-api", "ai-test" -> "AI Discovery"
    "home-control" -> "Home & remote"
    "about" -> "About"
    "licenses" -> "Licenses"
    "data-credits" -> "Data sources"
    "list" -> "List"
    "order" -> "Reorder"
    "inputs" -> "Inputs"
    "app-options" -> "App options"
    else -> when {
        route.startsWith("wallpaper-") -> "Wallpaper setup"
        route.startsWith("simkl-") -> "SIMKL setup"
        route.startsWith("home-") -> "Home setup"
        else -> "Settings"
    }
}

/** Identity, not the old position, owns a returning card/menu focus. */
internal fun matrixRestoreIndex(ids: List<String>, preferred: String?): Int =
    ids.indexOf(preferred).coerceAtLeast(0)

internal fun matrixBackResetsList(route: String, page: Int, atFirst: Boolean): Boolean =
    route == "list" && (page > 1 || !atFirst)

/** Hidden results must never receive a Back/restore focus request. */
internal fun matrixResultsVisible(count: Int, loading: Boolean, error: String): Boolean =
    count > 0 && !loading && error.isBlank()
