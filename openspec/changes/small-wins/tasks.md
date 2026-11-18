# Tasks

1. Capabilities (pure, new `MatrixCapabilities.kt`): a `MatrixCapabilities`
   data class (`animations`, `wallpaper`), `FULL` / `CHEAP` values, and a pure
   `decide(safeMode, lowRam)` returning `CHEAP` when either is true, `FULL`
   otherwise. A `LocalMatrixCapabilities` CompositionLocal defaulting to `FULL`.
2. Capabilities wiring: in `ComposeHomeActivity`, compute the capabilities in
   `onCreate` right after safe mode from `ActivityManager.isLowRamDevice`,
   provide them in `setContent` next to the existing inspection local, and
   replace the two ad-hoc `safeMode` checks on the wallpaper path (the 60 s
   refresh loop and `wallpaperChanged()`) with `capabilities.wallpaper`.
3. Glow gating: `rememberMatrixFocusRotation` gains an `enabled: Boolean = true`
   parameter; when disabled it holds at 0 and never animates. `MatrixArtwork`
   reads `LocalMatrixCapabilities.current.animations` and passes it through.
   No change to the 12 s sweep, the outline, or the scale on full-effects TVs.
4. Memory pressure: in `MatrixComposeApplication`, override `onTrimMemory` to
   clear Coil's memory cache at `TRIM_MEMORY_RUNNING_LOW` or above, and
   `onLowMemory` to clear it. Disk cache untouched.
5. Wallpaper cap: in `MatrixHomeSurface`, build the backdrop `AsyncImage` model
   as an `ImageRequest` sized to the view in pixels, capped at 1920×1080.
   1080p TVs decode at view size as before.
6. Per-channel cap: in `TvFeed.read`'s `preview_program` loop, skip the
   duplicate scan and `best()` call for a row already at `FeedRules.MAX_ITEMS`.
7. Top-metas limit: in `CinemetaClient`, extract the array parse into a
   testable `parseTopMetas(json, limit)`; `getTopMetas(type, limit)` defaults
   to unlimited; `LiveDynamicSuggestionsSource.topMetas` passes 2.
8. App-label cache: in `TvFeed.java`, cache `appName` results in a bounded
   static LRU (128 entries) with a `clearAppNameCache()`; call it from
   `ComposeHomeRepository.invalidateApps()`.
9. Tests: `MatrixCapabilitiesTest` covers the four `decide` combinations and
   the CompositionLocal default; `MatrixSearchTest` covers `parseTopMetas`
   keeping only the requested items. No new tests for the Android-only seams
   (trim, wallpaper size, channel cap, label cache) — verified by code reading
   and the full suite.
10. Build: full suite (`:app:testDebugUnitTest` + `:core:test`) plus `lint`
    with `--profile` in the fixes checkout; expect 207 + new tests green and
    lint 0 errors, 0 warnings.
