# Motion and held-D-pad review — 2026-11-04

This review targets Home row navigation, focus contrast and low-cost depth. Existing account/feed/Home-capture behavior is retained. Direct add-on catalogs remain unimplemented.

## Design references and choices

Apple's [focus guidance](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/) uses depth, scale and parallax. Its [materials guidance](https://developer.apple.com/design/human-interface-guidelines/materials) primarily separates controls/navigation from content. [Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass) describes native platform effects and hardware support; these are not Android APIs. Matrix keeps TV Material's focus scale, a separated rim and a cached diagonal highlight. Beta 43 removes the earlier dark contrast backing and adds two faint tinted gradient strokes as a feathered halo. It does not implement live backdrop blur, optical refraction, animated parallax or extra per-frame bitmap generation.

Android's [TV lazy-layout guidance](https://developer.android.com/training/tv/playback/compose/lists) supports scoped BringIntoViewSpec. Its [focus guidance](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior) supports focus groups. Current [BringIntoViewSpec source](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/BringIntoViewSpec.kt) deprecates custom scroll-animation overrides; no such override was added. [LazyLayoutCacheWindow](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/layout/LazyLayoutCacheWindow) supports prefetch/retention outside the visible viewport.

## Versioned experiments

| Build | Change | Result/status |
| --- | --- | --- |
| beta 36 | Separated focus rim | Visual contrast verified; stress navigation still inconsistent. |
| beta 37 | Always align vertical focus at 20%; focus groups; held-repeat backpressure; gradient rim | User reported clearer focus and stopping on release. Frame metrics did not improve. Alignment experiment rejected. |
| beta 38 | Remove constant pivot and its test; preserve minimum-distance reveal and useful focus/repeat work | Dump navigation/geometry pass; rendering remains janky. |
| beta 39 | Retain half a viewport ahead/behind in Home's Compose cache window | Installed and geometry/row-memory checks pass. Lower average focus time in sampled trace, but worst event still ~86ms and cache-specific churn benefit unproven. |
| beta 44 | Direct artwork measurement; observe row focus outside composition | Strict navigation/geometry pass. Small warm sample difference; insufficient evidence of a reliable gain. |
| beta 45 | Reuse Home's measured width instead of subcomposing every row | Strict navigation and unchanged slot geometry pass; warm sample improves, p95 stalls remain. |

Production cleanup removes superseded edge-sampling/contrast state from beta 35, beta 37's rejected pivot/test, and a redundant top-group state wrapper. Rendering and interaction remain in shared components. No new runtime library, animation-scale override or background polling was added.

## Diagnostics and reproducibility

`tools/matrix_ui.py` selects exactly one resumed Matrix activity. Explicit standard-app versus Home launches can leave a stopped second task; its old row data is not current UI. Two parser tests cover stale and ambiguous instances. No task-removal workaround was added to Home capture.

`tools/check_home_navigation.py` resets Home, sends 12 Down taps in a burst, paced Down to the last channel, 12 Up taps, synthetic longpress and another Home reset. It reads active window/activity dumps and frame counters, saves local artifacts and takes no screenshots. Bursts of zero-repeat taps are not equivalent to a continuously held physical remote. `--longpress` also emits only a limited repeat pattern on this firmware. XML UiAutomation can return a null root on this TCL, so app-scoped dumps are used as the fallback.

Physical feedback on beta 37: user confirmed clearer focus and stopping on release. A subsequent dump showed 386 held-repeat events, 172 deferred repeats and scrolling=false after release. Counters are cumulative, not a claim that all events occurred in a three-second interval.

Short gfxinfo stress samples (not frame-perfect identical traces):

| Build/condition | Frames | Janky | Median | p95 |
| --- | ---: | ---: | ---: | ---: |
| beta 36 first | 184 | 26.63% | 40ms | 200ms |
| beta 36 warm | 169 | 25.44% | 34ms | 150ms |
| beta 37 first | 173 | 31.21% | 40ms | 150ms |
| beta 37 warm | 130 | 32.31% | 40ms | 150ms |
| beta 38 first | 176 | 35.23% | 44ms | 250ms |
| beta 38 warm | 160 | 30.00% | 40ms | 200ms |
| beta 38 after compile request, same process | 188 | 25.53% | 38ms | 150ms |
| beta 39 first, compile requested before launch | 163 | 30.67% | 36ms | 200ms |
| beta 39 warm | 163 | 25.77% | 40ms | 109ms |

Counts and results vary with focus timing, cache state, compilation and possible real-remote input. Repeated input is deliberately bounded; these numbers do not prove a universal FPS or attribute an effect to compilation. GPU percentile sentinel values are excluded. The earlier 7.89% short navigation sample uses a different sequence and must not be compared as if it were this stress test.

## Trace evidence

An 18-second trace was recorded on beta 37 using the official Perfetto recording helper and app-jank config (scheduling, gfx/view/input and frame timeline). Private 33MB trace/SQL outputs remain under ignored artifacts. A delegated review followed the Android profiler triage workflow with pinned official trace_processor tooling.

- [SQL] No data-loss/error rows were returned by the integrity query used in triage.
- [SQL] Worst observed FocusOwnerImpl:dispatchKeyEvent: 87.003ms, with 81.963ms executing on CPU. Nested measure/layout: 82.128ms, 77.201ms CPU.
- [SQL] That subtree contained 17 Compose recompositions and image painter lifecycle work. Totals are inclusive/nested and must not be added as independent costs.
- [SQL] Across the trace, 662 Compose deactivations and 820 onForgotten slices were recorded. Measured RenderThread draw max was 12.48ms; bitmap decode was observed off-main.
- [INFERRED] Repeated focus/layout/composition setup is a stronger optimization candidate than adding visual effects or assuming the border is GPU-bound. Adjacent-row retention is a hypothesis to test, not a proven root-cause fix.
- [GAP] No JIT/GC/class-loading slices establish compilation as the cause. This trace does not prove absence of every GPU/system bottleneck or describe every user journey.

The beta 39 follow-up used the same recorder/config and burst sequence. Focus dispatch averaged 14.807ms versus 17.664ms in beta 37 (160 calls in each); max was 85.775ms versus 87.003ms. Layout averaged 6.924ms versus 7.416ms; its maximum remained 77.444ms. Deactivation counts were 678 versus 662 and forgotten compositions 800 versus 820, so the trace does **not** show a clear cache-specific reduction in disposal. Different alignment/compilation/cache conditions prevent attributing the lower average solely to cache retention. The new trace omitted process names; thread/process identity was resolved using its main thread ID and matching Matrix SurfaceFlinger layer. No positive integrity errors/loss stats were found in the query used. Root-cause work on worst-case focus/layout stalls remains open.

The follow-up trace also had unavailable-event setup notices, so event coverage is not assumed complete despite the absence of positive loss/error counters.

Post-stress PSS snapshots: beta 38 109,927KiB (107.35MiB), beta 39 114,037KiB (111.36MiB), approximately 4MiB apart. These are process snapshots, not an isolated cache allocation or matched idle/leak test. Stopped Home/standard instances may coexist; do not compare these stress numbers to an earlier single-activity idle measurement.

Final beta 39 independent row-memory check: Nuvio row card 2 → adjacent row card 1 → Up restores card 2 → Down restores card 1. Back resets the current row to zero, then returns to favorites zero. Toolbar frames and settled whole-card bounds pass. Only one final visual capture was used after the navigation runs; private trace/capture/dump files were not published. Physical held-repeat feedback was collected on beta 37's identical repeat handler, not retested as a new claim on every later APK.

## Halo/loading follow-up — beta 43

The artwork-to-rim gap remains 2dp and artwork inset 5.5dp. Two low-opacity cached gradient strokes replace the dark outer backing; blue/white artwork was visually checked after dump-based navigation checks. Warm app compositions seed directly from the existing banner cache; uncached media crossfades for 120ms, while Coil memory hits skip that transition. No cache growth, new dependency or live blur was added.

Animated Home reset experiments in betas 40/41 were removed after an early-input race. Restoring the previous reset code alone in beta 42 did not resolve Back: a consumed Back-to-first request replayed when a lazy row was reconstructed. Beta 43 clears that one-shot request after handling it. The strict row-memory and two-stage Back sequence now passes, as do Settings return and repeated injected Home checks.

Beta 43's first navigation stress sample recorded 158 frames, 32.91% janky, median 42ms and p95 200ms. Its warm repeat recorded 175 frames, 22.86% janky, median 36ms and p95 150ms. No explicit compilation request was made. These are separate loading conditions, not a controlled causal A/B against beta 39. The smaller warm jank percentage does not prove the worst stalls improved; further CPU focus/layout work remains necessary. Physical key behavior was not freshly retested in this pass.

## Shared measurement follow-up — beta 45

Google's current [Compose layout performance guidance](https://github.com/android/skills/blob/main/jetpack-compose/jetpack-compose-performance/references/layouts.md) recommends avoiding per-lazy-item subcomposition when constraints only determine measurement. Beta 44 replaces shared artwork's BoxWithConstraints and an extra wrapper with a [layout modifier](https://developer.android.com/develop/ui/compose/layouts/custom), measuring the fitted child once without changing the outer aspect slot. Beta 45 passes Home's measured width to the shared row, removing another subcomposition per favorites/channel row while preserving the same pixel-based fitting function. The only responsive BoxWithConstraints in this Home path is the parent.

Row focus is now observed inside snapshotFlow/collectLatest rather than as Home's LaunchedEffect key. This retains cancellable top-return behavior without invalidating Home composition for every row transition. Explicit Home/Back resets, native focus restoration/scale, bounded caches and the halo remain unchanged. Google's [TV migration guidance](https://github.com/android/skills/blob/main/tv/leanback-to-compose-tv-migration/SKILL.md) warns against directional overrides targeting lazy off-screen items; none were added. No blanket stability annotations, new framework, polling or global animation-speed override.

| Same bounded harness / condition | Frames | Janky | Median | p90 | p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| beta 43 preceding baseline | 157 | 25.48% | 38ms | 105ms | 150ms |
| beta 44 first | 158 | 27.22% | 42ms | 89ms | 150ms |
| beta 44 warm | 162 | 24.69% | 36ms | 81ms | 150ms |
| beta 45 first | 171 | 25.15% | 42ms | 101ms | 200ms |
| beta 45 warm | 194 | 20.62% | 32ms | 69ms | 150ms |

The warm sample improves, but its actual frame count/focus path differs and p95 remains 150ms. This does not prove causation or eliminate worst-case jank; no new CPU trace was recorded for this follow-up. After automatic tests stopped, the user tested favorites and held Down/release on beta 45 and reported smoother movement that stops promptly. Strict row memory/two-stage Back, seven-row traversal and whole-card/icon geometry pass. Physical-pixel row sizing matches beta 43 exactly. Final app/portrait captures confirm the same fitted corners, gap and halo. Private evidence remains under artifacts/motion44 and artifacts/motion45; no publication.

## ART compilation conditions (earlier experiments)

After beta 38 installation, dumpsys reported `verify` / reason `install`. An app-only `cmd package compile -m speed -f com.matrix.launcher` returned Success, but the actual reported filter was `speed-profile` / reason `cmdline`. Report the actual firmware result. The running process was not force-stopped or rebooted; do not claim its code switched live or that compilation caused a timing change. Installer `-Compile` is now optional and runs before launch. ART-generated code consumes separate device storage; APK bytes and global animation scales do not change. See [ART configuration](https://source.android.com/docs/core/runtime/configure) and [ART service](https://source.android.com/docs/core/runtime/configure/art-service).

Beta 39's immediate post-compilation check still reported `verify` / reason `cmdline` despite Success. The command is therefore not treated as proof of AOT completion or a performance fix on this firmware.

Beta 53 separates scroll resets from Home-key routing: the scrolling harness uses explicit Matrix MAIN/HOME intents by default. Use `--home-key-reset` only for a separate synthetic-routing check; neither route certifies a physical remote. Automatic tests stop if Matrix loses foreground. See [audit validation](VALIDATION.md) for the failed injected-Home attempt and remaining jank.
