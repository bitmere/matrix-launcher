# Proposal — matrix-liquid-glass

**Status:** Phase 1 Run 1 foundation implemented; coordinator gates pending.
Toolchain: AGP 9.1.1, Gradle 9.3.1, compileSdk 37, Kotlin 2.4.21,
targetSdk 34, minSdk 26. Phase 0 PASSED compilation and bounded-effect
integration: one 18dp `hazeBlur`, one `hazeSource`, readable API ≤30 fallback
with 70% black fill. Physical-TV frame budget remains unmeasured (no TV in
this VM); FULL defaults off until measured. Only `haze` + `haze-blur` 2.1.0
are installed. `haze-glass` is not needed for this foundation.

Run 1 adds ownership, policy, tokens, analysis, shared preblur and JVM tests.
Run 2 owns surface adoption, header tokens, Now Playing, dialog blur-behind,
focus springs, search entrances and rendered Impeccable passes. No Gradle,
commit, push or PR is authorized in Run 1. Existing spike/toolchain changes
in this checkout are preserved.

**User decision (2026-11-28): LIVE backdrop blur** — controls must visibly
blur scrolling posters and changing content, not just wallpaper frost. That
decision selects the rendering architecture below.

## Why
The launcher already has glass-like pieces — `MatrixControlStyle.glass()`
(static vertical gradient), the `MatrixGlassBorder` rim, and the Now Playing
frost (2.7.13) — but each screen invents its own treatment, and none of them
blurs live content. The user asked for the whole launcher to feel like iOS
Liquid Glass with edgeless surfaces, light-shadow reflections, and ColorOS 17
fluid motion, all aware of the current wallpaper. This change builds one
shared glass system, and replaces the current eased focus tweens with spring
motion.

## Two different products — name them honestly
**Wallpaper frost** and **live scene blur** are not the same thing, and the
first draft conflated them. A bitmap computed once per wallpaper change can
only frost a surface with the wallpaper; it cannot blur scrolling posters or
moving rows. Blur plus tint plus a rotating rim does not implement refraction,
local luminance adaptation, or Apple's vibrancy, so a frost-only result must
be called "wallpaper-aware frost with glass-inspired lighting" — not Liquid
Glass. The user chose live blur, so this plan implements live blur for the
floating controls (below) and keeps honest wallpaper frost where live capture
is impossible or unaffordable.

## Architecture decision — live blur route
Two routes were evaluated against the user's decision:

- **(a) Extend the in-repo repaint pattern** (`NowPlayingFrost`,
  `NowPlayingCard.kt:143–164`: repaint the gradient backdrop + re-decode the
  wallpaper photo at mapped offsets into a card-sized layer, then
  `Modifier.blur(18.dp)` that layer's own content). It works only for
  **static, app-owned, re-renderable sources** — wallpaper photo and
  gradient. It cannot blur scrolling posters: the rows are composed in a
  `LazyColumn`, and repainting them would mean duplicating the whole row
  composition (double image loading/layout, scroll-sync lag, visible swim).
  **Verdict: retained for STATIC wallpaper frost and the Now Playing card,
  not the live-blur engine.**
- **(b) Haze 2.1.0** (`dev.chrisbanes.haze`, Apache-2.0, released 2026-11-02,
  Glass modules available but not installed; the spike uses only `haze` and `haze-blur`). `hazeSource` on the background content +
  `hazeEffect` on the foreground control captures the composed content in
  the same window, blurs it with RenderEffect on API 31+, and re-captures on
  invalidation — so posters scrolling under a floating control genuinely
  blur live. 2.1.0 fixed stale content while scrolling on API 31 (#1509)
  and sampling across sibling `ComposeView`s in the same window (#1718).
  **Verdict: recommended live-blur engine.** It does not by itself solve a
  different Android window (dialogs — see below), and its native Android
  backdrop backend remains experimental.

**Original Phase 0 acceptance plan (integration passed; TV measurement still open).** The spike must
prove, on a scratch branch: (1) Haze 2.1.0 compiles against the pinned stack
(Compose BOM 2026.03.00, tv-material 1.1.0, Kotlin 2.4.21 — Haze 2.1.0's
changelog records Kotlin 2.4.21 / Compose Multiplatform 1.12.2, so a version
skew is possible and must be checked, not assumed); (2) one bounded live
effect (floating header over scrolling rows) holds the frame budget on a
newer TV and degrades cleanly on the old TCL; (3) its API ≤30 fallback is a
readable translucent scrim (blur disabled by default there; the RenderScript
path is experimental and is **not** the default). **If the spike fails, the
honest fallback is the wallpaper-frost system (STATIC everywhere) and the
live-blur goal is re-scoped with the user — never ship a broken live blur.**

A structural consequence of live blur: the header is currently a
`LazyColumn` item (`MatrixHomeSurface.kt`), so it scrolls away and nothing
scrolls *behind* it. Live blur of scrolling posters requires the glass
controls to be **floating overlays above the scrolling list** (header hoisted
out of the `LazyColumn`, Haze source on the background stack: wallpaper +
dim + list). The Now Playing card stays a row item, so its treatment remains
wallpaper frost. D-pad focus order must be preserved when the header moves
(header focus first, then rows).

## Source / window model (defined now, not discovered during build)
- **One owner above both windows.** Wallpaper generation, decoded source,
  crop, dim metadata, and the bounded blurred result are owned at the common
  ancestor of `MatrixHome` and the settings/search `Dialog`
  (`MatrixHomeScreen.kt:143–152`), and passed down — `MatrixBackdropState`
  today is created inside Home and `MatrixPanel` installs its own synthetic
  provider, so both must change. A separate panel-local provider is kept
  only where scroll-edge fading relies on panel pixels.
- **One common image, root/window-relative offsets.** Every live-blur
  surface samples the same source image with window-relative coordinates; a
  per-control local crop independently stretched into each control will
  visibly swim. Never sample a glass surface's own output back into the
  source.
- **Invalidation by content/generation, not just URI.** Cache keys cover
  source generation/content identity, output aspect/size, density, crop,
  effective radius, and theme/dim configuration. File content can change at
  the same URI. Coalesce/cancel stale work; publish only the latest
  generation. Do not regenerate a photo preblur on every artwork-focus
  colour change.
- **Coordinates and edges.** Effective blur radius converts dp to source
  pixels: `sourceRadiusPx = radiusDp × density × sourceWidth /
  displayedWidthPx` (uniform scale). Blur with adequate source padding
  before clipping the destination, so edges do not smear into darkness or
  show seams.
- **Memory budget.** At most current + previous 480p-class outputs (~4 MiB)
  of additional CPU blur budget, with independently bounded worker scratch:
  854×480 ARGB ≈ 1.56 MiB; 1920×1080 ARGB ≈ 7.91 MiB. (Existing gradient
  LRU is 16 MiB, max texture 1280×720.) Drop references on trim/disposal;
  never recycle images still in use. **Count effects, not material
  instances:** one shared RenderEffect on three layers can still mean three
  offscreen passes. Start FULL with one bounded live effect; allow another
  only after profiling. **Zero live blurs on the old TV until measured.**
- **No per-frame bitmap work.** Never extract, scale, PixelCopy, or generate
  a bitmap on focus ticks, playback ticks, or animation frames. (The 2.7.14
  leak fix exists because per-tick bitmap extraction/rescaling leaked.)

## Dialog window boundary — resolved now, not in Phase 3
Settings and search live in a separate Android `Dialog` window
(`MatrixHomeScreen.kt:152`, full-size 0.42 black scrim, `MatrixPanel`
inside). A normal Compose blur cannot sample pixels in another window, and
Haze does not solve that on its own.
- **FULL-tier enhancement:** `FLAG_BLUR_BEHIND` + `blurBehindRadius`
  (API 31+, pixel radius) on the dialog window. It blurs the whole
  behind-window scene, not clipped to the inner panel; the 0.42 scrim stays
  on top. Hardware may not support cross-window blur, and battery saving,
  multimedia tunnelling, or minimal postprocessing can disable it at
  runtime — so support is **subscribed to** (listener registered, released
  on disposal), not checked once at API level.
- **Fallback (always present, from Phase 1):** wallpaper frost (the shared
  blurred-wallpaper bitmap drawn inside the panel) or an opaque panel —
  named honestly as frost/panel, never as "a snapshot of what is behind".
  Never `PixelCopy` a fresh snapshot per dialog open. STATIC/CHEAP tiers
  always use the fallback.
- **Not chosen now:** moving settings/search out of `Dialog` into an
  in-window overlay (which would let Haze blur work there, but rewrites
  page hosting, focus/back handling, and IME behaviour). Recorded as a
  deferred alternative with its cost, not a Phase 1 task.

## Degradation tiers (corrected)
| Tier | Eligibility and behaviour |
|---|---|
| FULL | API 31+ is **necessary, not sufficient**: also adequate memory and measured GPU performance. One bounded live effect to start; cross-window support is a separate flag. Ambient animation disabled when inactive / reduced motion. |
| STATIC | API 26+ with budget for one shared preblur generation. **No live blur, but static wallpaper frost remains** (the first draft wrongly said "no blur"). Short focus transitions may remain unless reduced motion is requested. No ambient sweep by default. |
| CHEAP | Safe mode, low-RAM, allocation failure, or user low-effects choice. No photo wallpaper / cache / live blur; solid readable surfaces and unmistakable static focus. Ornamental animations explicitly gated (today's CHEAP does not disable every animation). |

Material quality, motion policy, and cross-window support stay separate
flags even if exposed as three user-facing presets. API level alone cannot
classify the old TCL — its exact OS/GPU/RAM and blur support are unconfirmed.

## Contrast policy (P1, blocks shipping)
Measure the **final composited background under actual text** (wallpaper dim
+ material + tint + highlights + overlapping imagery). Targets: **≥4.5:1
ordinary text, ≥3:1 large text and essential focus/control boundaries**;
test small secondary labels at the stricter threshold. Apple's 35% dark
dimming is a *suggestion to consider* for bright content, not a universal
rule: white with just 35% black is ~sRGB 0.65, giving white text only
~2.44:1 contrast — below target. Test matrix: snow/white wallpaper, busy
city detail, saturated art, black wallpaper, empty/loading states, imported
themes. Two representative colours cannot bound local bright patches; if
local analysis is unavailable, choose conservative thickness, not
speculative transparency. Whole-panel opacity remains an allowed fallback.

## Material starting values (audit proposals, not Apple/OPPO constants)
| Role | Starting treatment |
|---|---|
| Header / control cluster | One material for a coherent cluster. Dark neutral base 0.60–0.75 alpha over the correctly aligned wallpaper sample; restrained wallpaper tint 0.04–0.08. Start 18dp effective blur, explore 12–24dp only after measurement. |
| Now Playing | Keep the 0.72 surface base; replace hardcoded blue 0.16 with ambient tint ~0.06–0.10. Artwork/text stay sharp. Child controls use flat overlays, not another backdrop capture. |
| Settings / menu / dialog panel | Dark neutral base 0.85–0.95 alpha, tint ≤0.04–0.06. Opacity 1.0 allowed for contrast, failure, or Reduce Transparency. Rows are thin overlays, not separate glass surfaces (no glass-on-glass). |
| Poster title / badge | Opaque artwork intact. Local dark title scrim ~0.70–0.90 where text overlays artwork, validated on composites. Badges get a stable high-contrast fill. No per-poster backdrop RenderEffects. |
| Edge light | Partial 0.5–0.75dp highlight at 0.15–0.30 alpha, weaker when unfocused. **Do not recreate the removed uniform white outer poster border.** |

## Scope
- Phase 0 spike (Haze compat + one-effect perf gate).
- Phase 1: hoisted glass source, `MatrixGlass` tokens, floating header with
  live blur, Now Playing unified onto the new tokens (frost), focus springs,
  dialog-boundary architecture + fallback, three-tier degradation, contrast
  policy, reduced-motion behaviour.
- Phase 2 (conditional on the corrected host): poster scrims/rims,
  settings/menus/search onto the resolved dialog host.
- Phase 3 (optional optical enhancements): cross-window blur tuning, orb
  contour light, motion setting. Dialog fallback, motion gating,
  transparency fallback, and contrast requirements may **not** be deferred
  here — they are Phase 1.
- Plain JVM tests for the new pure helpers (tint/luminance, spring specs,
  capability tier selection) and device verification gates.

## Non-goals
No minSdk change (stays 26). No glass on poster artwork itself in the
default plan (posters stay opaque; see the open question on full glass on
cards). No haptics (TV has none). No lock-screen style transitions, no
per-app glass themes, no changes to core, backup schema, SIMKL/AI
integrations, or the updater. **Preference question resolved as open:**
Phase 1 relies on system Reduce Transparency / Reduce Motion / Increase
Contrast plus nonpersistent defaults; adding persistent in-app Reduce Motion
/ Reduce Transparency settings (with backup handling) is a user decision —
the first draft's "no preference keys" non-goal contradicted it, so it is
removed as a blanket non-goal and replaced by this open question. No Gradle, compilation, installation, commit, push, or PR in Run 1.

## Open questions (for the user)
1. **Cards:** frost-only; no card changes in Run 1.
   The audit corrected the rationale: Apple's rule is about the
   control/content layer, not a ban on anything shaped like a card — HIG
   allows transient active controls inside content, and tvOS adopts glass on
   focus. The default stays frost-only on cards; full glass on cards is a
   deliberate deviation, decided after seeing Phase 1.
2. **Old TCL facts:** exact model / Android API / memory class / output
   resolution. Needed before assigning STATIC vs FULL to that TV.
3. **Tint source resolved:** selected wallpaper only. Focus artwork is a separate stream.
4. **Persistent settings:** may Phase 2/3 add persistent in-app Reduce
   Motion / Reduce Transparency settings (with backup handling), or rely on
   system settings + nonpersistent defaults?
5. **Settings focus:** preserve the existing 1.5dp border. The poster-border removal (2.7.15) is not
   authorization to remove every control border.

## Run 2 implementation and conservative qualification

Run 2 adopts Header, Now Playing and Panel tokens and shared wallpaper frost;
adds disposable dialog blur-behind, focus springs and foreground-trim recovery.
See `run2-review.md` for the changed-file map, pinned dependency inspection,
source-review findings and coordinator gates. Search entrances remain Phase 2.

FULL is now fed by **process-local measured evidence**, not a model list or SDK
inference. Qualification has two stages:

1. On adequate-memory devices, passively observe 120 naturally produced Home frames
   after a 5-second startup grace. Every frame must have positive total and GPU
   durations ≤8ms and no dropped FrameMetrics reports. No frames are requested and
   no work is added to the draw path. Missing/invalid/slow evidence closes the gate.
   Baseline collection expires at 65 seconds after registration when a callback
   next arrives; no callback means no promotion and no active work.
2. Only after Home reaches STOPPED **and the screen is off**, wait one second, then use one worker to submit
   at most 24 **offscreen** full-display RenderNode blur frames, 100ms apart, within
   a four-second coroutine deadline. Each source changes (256 colored tiles), with
   an 18dp RenderEffect. Submission, recording, image acquisition and a signalled
   GPU completion fence together must finish in ≤4ms for **every** sample. There
   are no warmup samples excluded from this requirement. Two ImageReader buffers,
   one outstanding submission, no bitmap generation/readback, no PixelCopy and no
   UI window effect during qualification. Returning to STARTED cancels and rejects
   an incomplete run. No probe runs alongside any visible UI, including ClockActivity or another app.
   Screen-on cancels/rejects unfinished work; screen-off while Home is already stopped
   can start the probe. An ordinary app switch never starts a benchmark.

Admission also requires API 33 (completion fences), memory class ≥256MiB,
non-low-RAM, non-safe-mode, viewport ≤1920×1080, power saver off, no thermal warning.
These checks only **reject**, never grant qualification. API 31/32 stay STATIC;
API 31 remains necessary for the effects but is not sufficient for this probe.
All 144 valid samples are required to pass `measuredGpu=true` into `decide`.
Evidence lasts only for this process/display geometry/density; it is never saved
as a preference or device allowlist. Missing fence support, OOM, exceptions, timeout,
interruption, unsupported measurement or any failed sample stays STATIC, with no
retry in the same process. Safe mode/low RAM/source-allocation failure still win.

After promotion, naturally occurring Home frames continue to be observed: a total
frame >16ms, GPU >8ms, missing/nonpositive timing, dropped report, power-saving or
thermal warning revokes FULL for the process. Power/thermal callbacks are removed
on destruction, as is the frame listener. Callbacks use cached power/thermal state;
no binder polling or bitmap work is performed per frame.

The probe bounds two 1080p output buffers (~15.82MiB) plus driver-owned RenderEffect
scratch; it is separate from the unchanged ~4MiB wallpaper output budget. Actual GPU
residency must be measured by the coordinator. Native driver submission cannot be
preempted by coroutine cancellation; there is at most one in flight, and completion
fence waits are capped at 4ms. A driver hang cannot be certified safe by application
code. This is a conservative **proxy workload**, not a claim of measured Haze or
SurfaceFlinger performance on this checkout. Final physical-device qualification
must still verify the real scrolling header and compositor paths; monitoring
revokes optimistic evidence. Thresholds are proposals, not device acceptance results.

Only one live effect is active in Home. Opening a dialog disables header Haze and
uses shared frost there; the dialog's system blur is the sole live enhancement.
Fallback material remains present through runtime cross-window support changes,
including battery-saving and tunneled playback. The deferred in-window alternative
would rewrite hosting/focus/Back/IME and is not implemented.

CHEAP uses an **opaque contrasting token base** (normally black; white if an imported
foreground requires it), not the spike's translucent 70% black. Missing/loading
frost uses that same stable solid fill. STATIC adopts the shared frost. FULL Home
uses 18dp Haze plus the same contrast-solved token fill and partial .75dp edge light.
Now Playing uses frost in every wallpaper-capable tier, with .72 minimum base and
.08 wallpaper tint. Contrast policy may raise opacity or suppress tint.
