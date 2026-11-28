# wallpaper-tint

## Purpose
Glass tint and luminance adapt to the current wallpaper. The first draft
claimed this could reuse "the pipeline that already exists" with no new
sampling — that was wrong, and this spec corrects it.

## Current state (verified in code)
- `ComposeHomeRepository.palette(uri)` (`ComposeHomeRepository.kt:45`)
  samples **any** URI into an `ArtworkPalette` (two colours) on IO: a 32px
  software Coil request, cached in a 128-entry LRU. It is generic — it can
  analyse the wallpaper URI, but today nothing calls it with the wallpaper.
- `MatrixAppearance.palette` holds the **focus-app** colour:
  `rememberMatrixArtworkFocus` samples the focused app's banner (coalesced
  80ms) and `focusApp(color)` derives a companion arithmetically
  (`ArtworkPalette.app(color)`). This is focus ambience, **not**
  selected-wallpaper or local-luminance analysis.
- The real wallpaper is painted in `MatrixHomeSurface.kt:118–128` as a
  full-screen `AsyncImage` (decoded at most 1920×1080) plus a vertical black
  dim (0.65 / 0.35 / 0.85). It lives **outside** `MatrixBackdropState`,
  which only holds synthetic gradient textures (`BackdropTextures`, 16 MiB
  LRU, max texture 1280×720). The panel branch already draws a "Liquid
  Glass Material 3 surface" — a fixed dark frost/sheen, not actual wallpaper
  blur and not tinted from the wallpaper.
- Wallpaper sources stay as they are (storage, Wikimedia, artwork colours;
  rotation 15/30/60/120 min).

## ADDED Requirements

### Requirement: Shared wallpaper-tint contract
Matrix SHALL implement the following requirements across the staged runs.
- **Add a separate wallpaper-analysis stream**, distinct from
  `MatrixAppearance.palette`. Reuse `repository.palette(wallpaperUri)` (the
  same 32px sampling, cached) and hold the result in its own state, owned
  by the hoisted glass source (see proposal). Never read the focus palette
  as if it were the wallpaper palette, and never regenerate a photo preblur
  on every artwork-focus colour change.
- Derive, per wallpaper change and off the main thread: glass tint =
  wallpaper colour at low alpha; a luminance value (light vs dark
  wallpaper) from the same sampled colours. **Two representative colours
  cannot bound local bright patches** — where local analysis is
  unavailable, choose conservative thickness (see the contrast policy in
  the proposal), not speculative transparency.
- Produce one shared blurred-wallpaper bitmap per wallpaper change for the
  STATIC tier: downscaled (480p-class), blurred once, cached alongside the
  existing gradient textures, exposed through the hoisted glass source.
  Every frost surface draws from this one bitmap. Invalidation follows the
  source-generation key in the proposal (file content can change at the
  same URI).
- No per-card blur, no per-frame bitmap work, no new decoding on the focus
  path. Palette updates must not recompose cards (today cards sample the
  palette in the draw phase for exactly this reason — keep that).
- A wallpaper change re-derives tint + blur once, off the main thread, then
  crossfades (the 320ms backdrop crossfade stays a fade).
- **Resolved user decision:** tint follows the selected wallpaper only. Focus ambience remains separate; no combination is built.
- Light-wallpaper legibility is verified on the TV for both tiers (FULL and
  STATIC) against the contrast policy; if the scrim rule cannot hold
  contrast, the surface falls back to the thicker fill instead of shipping
  unreadable text.

## Delivery boundary
Run 1 implements source/policy/token foundations only. Run 2 adopts these on
surfaces and performs rendered/device validation; existing settings behaviour,
1.5dp focused borders and navigation remain intact. Persistent in-app motion
and transparency preferences are explicitly out of scope.

#### Scenario: Unmeasured device
- GIVEN a device without a measured GPU qualification
- WHEN selecting material quality
- THEN use STATIC wallpaper frost, or CHEAP for safe mode, low RAM,
  allocation failure or a nonpersistent low-effects choice
- AND do not enable live blur merely because API 31 is available.
