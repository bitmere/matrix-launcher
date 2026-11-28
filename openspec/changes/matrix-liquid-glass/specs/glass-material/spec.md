# glass-material

## Purpose
One shared definition of "glass" for the whole launcher, so every surface
uses the same tint, blur, edge light, and scrim — instead of each screen
inventing its own gradient (today: `MatrixControlStyle.glass()` is a static
vertical white gradient, and `NowPlayingCard` carries its own blue tint).

## Definitions
- **Control glass**: a translucent surface floating above content (floating
  header, Now Playing card, menus, dialogs). The only tier that gets real
  backdrop blur — **live** blur in FULL, wallpaper frost in STATIC.
- **Content frost**: the treatment for posters/cards. Artwork stays opaque;
  only the title scrim, badges, and focus rim are glass-treated. "Frost"
  here means stable scrim/badge/rim styling, **not** independently blurred
  content — there is no per-poster blur.
- **Live blur**: the control visibly blurs whatever is behind it *right
  now*, including scrolling posters (user decision 2026-11-28). Delivered
  by Haze 2.1.0 source capture in the same window (see below).
- **Wallpaper frost**: a static blurred copy of the wallpaper, computed once
  per wallpaper change. Honest name for what the repaint pattern delivers;
  never advertised as live scene blur.
- **Edge light**: a bright specular stroke on the lit side of a surface and
  a soft inner shadow on the far side. An aesthetic preference, not a
  readability law — "light defines the edge, never a dark border" describes
  the look, it does not override the contrast policy.
- **Scrim rule**: text legibility comes from the measured final composite,
  never from darkening the whole card and never from a fixed dimming
  percentage. Apple's 35% dark dimming is a suggestion to consider for
  bright content, not a universal rule (white + 35% black ≈ sRGB 0.65 gives
  white text only ~2.44:1 contrast — below the 4.5:1 target).

## ADDED Requirements

### Requirement: Shared glass-material contract
Matrix SHALL implement the following requirements across the staged runs.
- Glass tokens live in one place (`MatrixGlass`, next to
  `MatrixControlStyle` in `MatrixButton.kt:59`) and are derived from the
  wallpaper analysis stream (see `wallpaper-tint`): tint = wallpaper colour
  at low alpha; luminance decides the scrim. Starting values (audit
  proposals, not constants): header base 0.60–0.75 alpha, tint 0.04–0.08,
  blur start 18dp effective, explore 12–24dp only after measurement; Now
  Playing base 0.72, ambient tint 0.06–0.10; settings panel base 0.85–0.95,
  tint ≤0.04–0.06, opacity 1.0 allowed for contrast.
- **Live-blur engine: Haze 2.1.0** (`dev.chrisbanes.haze:haze`,
  `haze-blur`, both pinned to 2.1.0; `haze-glass` is not installed). `hazeSource` on the
  background stack (wallpaper + dim + list), `hazeEffect` on the floating
  control. Phase 0 proved compilation against
  the pinned stack (BOM 2026.03.00, tv-material 1.1.0, Kotlin 2.4.21 — Haze
  2.1.0 records Kotlin 2.4.21 / Compose Multiplatform 1.12.2) with one bounded live effect. Physical-TV frame-budget measurement is still pending; FULL defaults off. Haze does not solve the
  dialog window boundary; its native Android backdrop backend is
  experimental and is not relied on.
- **Repaint pattern kept for STATIC frost and Now Playing**: repaint the
  shared backdrop into the surface, then blur that copy — produced once per
  wallpaper change and cached, never per card and never per frame. It is
  **not** the live-blur engine: it can only re-render static, app-owned
  sources (wallpaper photo, gradient), not a scrolling `LazyColumn`.
- **Source model:** one common source image owned above Home and the dialog
  host, sampled with root/window-relative offsets. A per-control local crop
  independently stretched into each control will visibly swim. Invalidation
  by content generation (see proposal). Blur radius converts dp to source
  pixels with padding before clipping (`sourceRadiusPx = radiusDp ×
  density × sourceWidth / displayedWidthPx`).
- **Budgets:** at most 2–3 blurred layers visible per screen as an
  experimental ceiling (1–2 on low-end hardware), never blur list items or
  whole screens, never animate blur radius on a large surface — animate
  alpha or transform instead. Start FULL with **one** bounded live effect;
  allow another only after profiling. Count effects, not material
  instances: one shared RenderEffect on three layers can still mean three
  offscreen passes. Memory: at most current + previous 480p-class outputs
  (~4 MiB) additional CPU blur budget. Zero live blurs on the old TV until
  measured.
- **No stacked blurs:** one blur layer, tinted, is enough. A surface cannot
  sample another glass surface's output, and glass output must never be fed
  back into the source.
- `Modifier.blur` blurs a composable's own content, not what is behind it;
  below API 31 it is silently ignored. An ignored blur is **not** a preblur
  fallback — the STATIC tier selects a real CPU preblur (bounded, off the UI
  thread, on source change) or a readable opaque/near-opaque surface.
  Never use deprecated RenderScript as the default.
- Corner radii stay concentric with the existing tokens (panel 24dp, card
  14dp); glass does not invent new radii. Theme focus-scale/radius
  configuration is preserved, not hardcoded.
- **Contrast acceptance (P1):** ≥4.5:1 ordinary text, ≥3:1 large text and
  essential focus/control boundaries, measured on the final composite (see
  proposal test matrix). Whole-panel opacity remains an allowed fallback.

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

## Run 2 requirements delivered

- Header selection is explicit: FULL live Haze; STATIC shared `drawFrost`; CHEAP
  opaque contrast base. Missing source also gets solid. Position, first-in-focus
  traversal, measurement and list content padding remain identical.
- Now Playing uses the shared preblur and token material, .72 minimum base/.08 tint.
  Its separate decode, card RenderEffect and hardcoded blue .16 layer are removed.
  Child commands remain flat MatrixIconButton overlays.
- One live effect at a time: header Haze is disabled when the dialog obscures Home.
- Qualification MUST require both the 120-frame ≤8ms passive baseline and 24 completed
  ≤4ms offscreen blur submissions described in the proposal. SDK/memory checks only
  reject candidates. Partial/invalid/failing measurements MUST leave STATIC.
- Foreground trim clears retained images and changes a refresh key so the serialized
  worker regenerates without waiting for a lifecycle resume. STOPPED still suspends
  generation and clears sources. Output budget and worker mutex are unchanged.

#### Scenario: Qualification then revocation
- GIVEN valid baseline and completed offscreen measurements in this process
- WHEN the lifecycle returns to Home with unchanged geometry and safe thermal/power state
- THEN the activity passes measuredGpu=true to the capability decision
- AND any subsequent bad measured frame or unsafe power/thermal state revokes FULL.

#### Scenario: Return during qualification
- GIVEN the offscreen worker is collecting evidence after STOPPED
- WHEN the activity reaches STARTED
- THEN cancel the worker, discard partial evidence and remain STATIC for the process.
