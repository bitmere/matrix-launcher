# Tasks — matrix-liquid-glass

## Phase 0 — passed integration; device measurement outstanding
- [x] Compile Haze 2.1.0 haze + haze-blur on migrated toolchain (coordinator evidence).
- [x] One bounded 18dp hazeBlur and one hazeSource; API ≤30 70% black fallback.
- [x] Verdict: adopt Haze; no haze-glass module added.
- [ ] Measure physical TV frame budget; qualify GPU before enabling FULL.

## Phase 1 Run 1 — foundation
- [x] Read corrected draft, audit, current implementation and OpenSpec format.
- [x] Hoist wallpaper source and Home backdrop above Home/Dialog.
- [x] Retain panel-local provider for menu/grid scroll-edge pixels only.
- [x] Define generation/content identity, size/aspect, density, crop, radius, theme/dim key.
- [x] Bound current/previous blur outputs and serialized worker scratch; trim/disposal cleanup.
- [x] Pure WCAG luminance/composite/contrast helpers and conservative scrim solver.
- [x] FULL/STATIC/CHEAP, separate motion and cross-window flags; FULL defaults closed.
- [x] Observe system disabled animations; nonpersistent defaults; gate ornaments.
- [x] MatrixGlass role tokens; selected-wallpaper tint separate from focus ambience.
- [x] Reuse 32px palette sampler; shared off-thread CPU preblur with 320ms fade state.
- [x] Add 10 GlassPolicy JVM tests; expand capabilities from 5 to 8 tests (net +13).
- [x] Run 1 coordinator gates: 349 tests/0 failures, lint 0 errors, assembleDebug and structure passed (user-supplied evidence).

## Phase 1 Run 2 — surface implementation
- [x] Measured closed-by-default GPU qualification and activity wiring; thresholds in proposal.
- [x] Header tokens and FULL live / STATIC shared frost / CHEAP opaque treatments.
- [x] Now Playing shared frost and wallpaper ambient tint; flat child controls.
- [x] Panel shared frost and exact matching panel-local scroll-edge pixels.
- [x] FLAG_BLUR_BEHIND runtime subscription, teardown and always-present fallback.
- [x] Focus scale/glow springs; pinned TV Material binary inspected (source access deviation recorded).
- [x] Preserve press/theme targets; no double scale; unchanged reveal and long press.
- [x] Gate sweeps on unobscured Home and normal-motion/resumed lifecycle.
- [x] Foreground trim refresh without waiting for next resume; unchanged memory/worker bounds.
- [x] Add 11 pure JVM tests for qualification, treatment and focus/dialog policy.
- [x] Impeccable source audit/polish passes and detector; structure and whitespace review.
- [x] Update proposal, specs and Run 2 review with deviations and evidence limits.
- [ ] Coordinator diff review before compilation; focused/full JVM, lint and build gates.
- [ ] Physical GPU/header and compositor validation, rendered contrast, XML/D-pad, glyph centering.
- [ ] Search entrance treatment (explicitly deferred to Phase 2).

## Later phases / device gates
- [ ] Frost-only poster titles/badges; no uniform white poster border.
- [ ] Full composite contrast: snow, city, saturated art, black, loading, imported themes.
- [ ] Old TCL: zero live blur until measured; memory and frame timing on both TVs.
- [ ] Repeated source changes, same-path rewrite, trim, failure, disposal, motion changes.
- [ ] Cross-window support changes, tunneled video, keyboard and rapid page switching.
- [ ] Any persistent motion/transparency settings need a separate user decision.

No Gradle, compilation, JVM execution, installation, commit, push or PR in Run 1.

## Run 1 handoff and limits (historical; Run 2 supersedes surface and trim notes)
- Structure audit passed (17 manifest components, core Android-free); diff whitespace passed.
- Impeccable: seven Kotlin files scanned, no regex findings; parsed contrast rules do not run on Kotlin.
- Source review kept focus/nav/IME unchanged. No rendered or device acceptance is claimed.
- Source owner is above both windows. Home uses its decoded photo. MatrixPanel's
  local backdrop carries the same source reference but retains synthetic panel
  pixels for MatrixScrollEdges (menus/grids); Run 2 adopts the frost renderer.
- `drawFrost` takes full-window bounds mapped into the destination canvas; the
  caller clips its own surface. It does not compare coordinates across windows.
- Current + previous blur outputs are each at most 854×480 ARGB, together
  ~3.13 MiB, checked against 4 MiB. Interrupted fades discard the previous output
  before another worker starts; the worker mutex serializes cancellation/replacement.
- Blur scratch: two pixel arrays and a working bitmap, at most ~4.69 MiB.
  Decoded source is independently bounded to 1920×1080 ARGB (~7.91 MiB).
  Decode/crop worker has at most two such images (~15.82 MiB); Coil photo caches
  are disabled for this path. These are CPU bounds, not measured GPU residency.
  Existing 16 MiB synthetic-gradient LRU is unchanged. Allocation failure switches
  the provider to CHEAP for its lifetime; trim drops references until resume.
- Wallpaper SHA-256 is streamed on IO during existing selection/polling, so equal
  paths, lengths and timestamps still detect replacement. Unchanged polls do not
  advance generation. Palette sampling keeps the 32px/128-entry policy with a
  generation-aware key; focus requests keep their independent URI cache path.
- The 320ms shared frost transition state is ready; Run 2 surfaces must draw it.
  Home's unblurred photo replacement remains immediate, as it was before Run 1.
- Header retains the spike's 70% fallback; Now Playing retains its original local
  repaint/decode until Run 2. The new token solver is not a claim that these older
  surfaces, secondary text or imported-theme composites already pass P1 contrast.
- FULL requires explicit measuredGpu=true. No runtime device qualification source
  is installed in this VM; cross-window support stays false until Run 2 subscribes.
- System animation-duration-disabled is observed live; ornament loops and new
  backdrop transitions are gated. Existing library focus animations retain Android /
  Compose system duration-scale handling; spring migration remains Run 2.
