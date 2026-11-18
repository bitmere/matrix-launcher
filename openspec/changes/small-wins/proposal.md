# Small wins: old-TV-friendly performance bundle

## Why

Matrix runs on both old (1–2 GB RAM, 1080p, slow storage) and new (4K, more RAM)
TVs from one codebase, and the codebase must stay small, modular, and
understandable. This bundle collects the small, verified wins from the
2026-11-14 launcher survey (FLauncher, Projectivy, ATV Launcher, Leanback)
plus two fitting items from the dead-code diagnosis (D-08, D-15) — each one
small, each one in one file or area, no new data model, no new dependency.

Two items the survey suggested turned out to be already done, verified from
code — they are recorded here and need no change:
- Refresh apps on package broadcasts, not on every resume (ATV #4): the Home
  activity already registers a package broadcast receiver that calls
  `invalidateApps()` + refresh, `installedApps()` caches by generation, and
  `onResume()` does not re-query. Already satisfied.
- Minute-aligned clock ticker (ATV #10): the Home clock already delays to the
  next minute boundary (`delay(60_000L - System.currentTimeMillis() % 60_000L)`).
  Already satisfied.

## Scope

1. **Capability-gated effects (#7, the foundation).** One new pure object,
   `MatrixCapabilities`, decides from safe mode + `ActivityManager.isLowRamDevice`
   whether Home may use animations (the focus-glow sweep) and wallpaper.
   Safe mode or a low-RAM device gets the cheap variant; everything else gets
   full effects. The activity computes it once, provides it via CompositionLocal,
   and the two ad-hoc safe-mode checks on the wallpaper path use it instead.
   Existing safe-mode gating for SIMKL/AI discovery is untouched (feature
   gates, not effects).
2. **Memory-pressure eviction (#3a).** `MatrixComposeApplication` clears Coil's
   memory cache on `onTrimMemory` at `RUNNING_LOW` or above, and on
   `onLowMemory`. The disk cache (64 MB) is untouched.
3. **Wallpaper decode cap (#3b).** The Home backdrop request decodes at most
   1920×1080 (the view size on smaller screens, capped on 4K). 1080p TVs are
   unaffected; 4K TVs stop allocating a full 3840×2160 backdrop bitmap.
   The banner path is already banner-first and already draws into a capped
   480×270 canvas, so it needs no change; Coil's disk cache already exists.
4. **Per-channel program cap (#8).** In the `preview_program` scan, a channel
   row that already holds `FeedRules.MAX_ITEMS` (12) skips the duplicate scan
   and the re-sort for further programs. A worst-case bound, not a measured
   win: it only fires for channels publishing more than 12 programs.
5. **Top-metas parse limit (D-08).** `CinemetaClient.getTopMetas` parses only
   the first N items; the live suggestions source passes 2 (the only items the
   suggestions builder uses). The download cap (256 KB) is unchanged.
6. **App-label cache (D-15).** `TvFeed.appName` caches labels in a bounded
   static LRU (128 entries) instead of a PackageManager Binder call per row
   per Home load. The cache is cleared by `invalidateApps()`, the same event
   that invalidates the app list, so a renamed/updated app is re-read on the
   next package broadcast.

## Non-goals

- No shy mode / debounced backdrop fetching (#4), no cached app inventory
  (#1 full), no two-pass startup (#2), no categories/folders (#5), no parental
  controls (#6), no D-06 toggle, no D-22 — each is a follow-up change.
- No change to what channels are read or how rows are ordered; no change to
  the glow's 12 s sweep, the focus scale, or the backdrop on full-effects TVs.
- No new dependency, no new permission, no new preference key, no new data
  model. No Haze blur, no plugin API, no app catalog.
