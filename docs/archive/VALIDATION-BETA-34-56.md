# Historical beta validation

These entries describe their original test runs and publication status. Retained fixes are included in stable 1.1.0.

# Local 1.0.1-beta.56 - global warm Nuvio title routing

VersionCode 84. Optimized APK 1,776,382 bytes; installed SHA256 `f0998dd79f67110b3620034b8f66e82ab08580ed18db13fb69e35e0ef786ebb9`. All 133 JVM tests pass. Final Checks build passes; lint has zero errors and the existing orientation warning. No new dependency or permission.

Actual Matrix cards verified warm movie-to-movie-to-series, Search-to-SIMKL Home, then SIMKL-to-Home-to-different-Search-title. Visual endpoints matched selected titles. One shared compatibility route avoids installed Nuvio beta 10's single-top detail-state reuse. [Implementation, version boundary and corrected harness failures](../NUVIO-ROUTING.md). Continue Watching context preservation is reviewed; full playback is not certified by these detail tests.

Automatic input stopped on Matrix. Accounts, Home capture and profile preserved; no Play selected. The rejected task-reset diagnosis experiment required returning to the already authorized HYD home profile; task clearing is not shipped. APK/source remain unpublished; GitHub beta 53 unchanged.

---

# Local 1.0.1-beta.55 — remaining fixes installed and reboot verified

VersionCode 83. Optimized APK 1,776,382 bytes; installed SHA256 `717e2f99441830b0501cb2e54881a95a1a7775fbab89f43c9edcd849d3132b95`. All 130 JVM tests pass, including four filesystem cache tests and the existing 10,000-case geometry coverage. Two parser tests, structure and whitespace checks pass; lint has zero errors and the existing orientation warning. No new permission or dependency. Final optimized build took 157.6 seconds.

[Implementation, trace comparison, retention policy and compilation recipe](../OPTIMIZATION-54.md). The visible-count prefetch experiment in beta 54 was rejected after worse tail latency. Beta 55 retains default nested prefetch, slightly more already-visited rows and a tiny geometry cache. Warm matched-trace focus mean was 8.831 ms versus beta 53's 12.254 ms; maximum 61.487 versus 70.716 ms. The first beta 55 trace was worse and is retained as evidence of runtime/warmup variance. Urgent-prefetch and frame-timeline outliers remain; smooth 60 fps and isolated per-change causality are not claimed.

Before reboot: all seven rows, strict independent row memory, first-card/Home Back, whole settled slots, centered icons and 40 playback-free menu/integration route checkpoints pass. Post-warm memory snapshot 104,399 KiB PSS (~102 MiB), with one Activity; beta 53 had two Activities, preventing a causal RAM-reduction comparison.

After trace capture, SAVE_PROFILE returned 12, app-only speed-profile compilation returned Success and actual ABI state reported speed-profile/cmdline. The authorized reboot completed at ~30 seconds uptime when ADB first confirmed boot. Matrix was foreground without an injected Home, the Home service was bound, recovery enabled, all seven rows present and the compiler state retained. Post-reboot traversal, geometry and final fitted-card/progress screenshot pass. Graphics stress sample 31/176 janky frames (17.61%, p95 85 ms), a distinct post-compile/reboot condition.

Automatic input stopped. The user confirmed repeated Home, Inputs and Settings work and navigation is smoother. No media or microphone activation, account/profile choice, source/favorite save, DNS change, global animation/dexopt setting or debugging toggle change. New cache retention applies only to regenerable snapshots; protected metadata/library/pinned-preview snapshots are retained. APK and source remain local/unpublished; GitHub beta 53 is unchanged.

Private evidence: artifacts/audit54 (traces, SQL, boot, diagnostics, screenshot, route/focus checks and source hashes). Install receipt uses the exact final optimized bytes. Publication requires a new user request.

---

# Local 1.0.1-beta.53 — component audit installed

VersionCode 81. Optimized APK 1,776,382 bytes; installed SHA256 matches `cf250d55737ad8c653923dd668542afda849dcca3d57680d130909eee3ca8cd1`. All 126 JVM tests pass (including 10,000 deterministic geometry/reveal cases and malformed SIMKL fixtures); two dump-parser tests pass. Lint has zero errors and the existing orientation warning. Structure audit, XML parsing (30 files), Python syntax and whitespace checks pass. Final optimized build: 78.7 seconds. No new dependency/permission or default-Home/account changes.

[Component audit and 77-file ledger](../CODE-REVIEW-53.md) records corrections and remaining work. Static source/resource/reference review found no additional unreferenced resource or removable platform class candidates. Confirmed duplication was consolidated in remote launch paths and custom-list mapping; no large dead class tree was claimed or removed.

Device: 40 playback-free route checkpoints across Settings, Favorites, Sources, row order entry/exit, sizing, wallpaper credits, SIMKL usage/list entry, TMDB credential cancel, Home controls, data credits, Licenses and Inputs. Pickers were cancelled; no Save/toggle/disconnect, microphone or media action was activated. SIMKL list entry was checked as a route; the sampled first list had no focused card at that checkpoint, so this is not a claim of populated pagination verification.

The first stress reset using injected HOME left Matrix on Google TV. Input stopped safely; accessibility dumps showed the Home service bound/enabled with no crashed-service entries. This is a recorded routing failure, not a passing physical Home-key test. The scrolling harness now uses explicit Matrix intents by default; --home-key-reset opts into the synthetic routing check separately. After automatic input stopped, the user confirmed repeated real Home presses consistently open Matrix. The failed injected-HOME route remains recorded separately.

Two explicit-intent Home stress passes both reached all seven channel rows with fast bursts, paced steps and synthetic longpress, then returned to Favorites. Settled checks: three rows with whole visible slots; six Home icon frames centered within 0.5 physical pixels. Search regression passed typed 24-result submission, lateral/vertical focus, Back to first then query, and cached-query refocus. Nine icon frames passed on Search; the final capture was visually inspected. No live TMDB credential or real voice retry in this pass (voice was user-confirmed on beta 51).

Performance remains unresolved: stress samples reported 36/163 janky frames (22.09%, p95 125 ms) and 36/173 (20.81%, p95 117 ms). These are bounded stress diagnostics, not an A/B benchmark or a claim of smooth 60 fps; the driver's 4950 ms GPU sentinel values are not interpreted as useful timing. One warm post-stress snapshot: total PSS 123,820 KiB (~120.9 MiB), two Activity objects, zero WebViews. Current-PID crash buffer was empty. These samples do not certify no leaks/ANRs or a memory improvement.

Artifacts remain private under artifacts/audit53 and artifacts/search53, including source hashes, route dumps, graphics/memory/crash diagnostics, install receipt and final capture. Automatic input stopped with Search open. The user subsequently authorized GitHub publication of beta 53. This prerelease uses the exact optimized APK and hash above; stable v1.0.0 remains unchanged.

---

# Local 1.0.1-beta.52 — installed, single-row Search

VersionCode 80. Optimized APK 1,776,382 bytes; installed SHA256 matches `3056328a61a1438d466d01828a923085b69657232e298c77a5c3c2caaf2f5e9a`. All 124 JVM tests and two parser tests pass. Lint has zero errors and the unchanged orientation warning. Structure audit and whitespace checks pass. Build: 75 seconds. APK size is unchanged from beta 51; no runtime speed claim is made.

The Search heading/breadcrumb and second input level are gone. One top row contains compact Back, query, submit and voice controls. Icon-only actions use MatrixIconButton; the text field retains shared form geometry. Results begin below this row. Final screenshot inspected for layout and glyphs; nine diagnostic icon frames are centered within 0.5 physical pixels. Scoped dumps verify 24 typed SIMKL results, lateral and vertical movement, Back to card zero then query, and first-card focus on cached resubmission after replacing the counter-based effects. No media or microphone was opened in this pass. Real voice recognition was confirmed by the user on beta 51; it was not re-recorded on beta 52. Live TMDB still needs a credential.

Cleanup removed three request-related state variables, their effect-trigger mechanism, duplicated header usage, an unused MediaRow result title/ID wrapper and a stale JSON import. Requests now belong to one composition-scoped cancellable job; cancellation, busy state and repeated cache-hit focus remain explicit. Measured Search-file diff from beta 51 is 34 lines removed and 30 added (four net removed), plus the wrapper/import removals in repositories. This is a structural simplification, not a claim of hundreds of dead lines removed.

The broader declaration/reference scan covered app Kotlin/Java and core/test references. Its single-use candidates were platform callbacks (input devices, network changes, notification listener, application image loader), manifest components or the cache eviction override; these remain required. This scan is not a complete semantic audit of every project line or resource. Private baseline copies and device checks remain under artifacts/cleanup52 and artifacts/search52. Automatic input has stopped with Search open; nothing committed or published.

---

# Local 1.0.1-beta.51 — installed, separate text and voice controls

VersionCode 79. Optimized APK 1,776,382 bytes; installed SHA256 matches `70fee8d44fde1b03db6bce4accdc9d4cd06f48a71cacd8301ba3985a24134b84`. 124 JVM tests pass with no failures/errors; lint has zero errors and the existing orientation warning. Structure audit, two parser tests and whitespace checks pass. Final build took 76 seconds. No new dependency or microphone permission; the manifest adds only a narrow recognition-activity visibility query.

The Search field no longer contains another focusable submit button. Separate labeled Search and Voice controls share existing Matrix button styling. Removed the obsolete trailing-action parameter, extra Row and unused imports from the shared field; setup forms retain their existing input flow. Remove TMDB credential is hidden without a credential. Single-provider naming moved into the input hint, while the dual-provider selector remains conditional.

The first automatic check stopped safely because the TV was still dreaming and no Matrix activity was resumed. After waking it, scoped dumps verified typed submission (24 SIMKL matches), lateral/vertical selection, Back to the first result then query, and cached resubmission refocusing the first card. The final movie screenshot was visually inspected for layout and separate text/voice controls. Search remains open; automatic input stopped without opening media or activating the microphone. System recognition activities resolve on this TCL. The user confirmed the real-remote Voice action returns matching results in Matrix. Cancellation through the external recognizer, missing-recognizer behavior on another TV and live TMDB remain unverified.

Private receipts/dumps/capture remain under artifacts/search51. Nothing committed or published. This scoped cleanup does not claim that every unrelated project file was audited or that a performance improvement was measured.

---

# Local 1.0.1-beta.50 — installed, search simplified

VersionCode 78. Optimized APK 1,775,746 bytes; installed SHA256 matches `616b5e1b8d53b87143fe149aa2e2b9e2e924a4db81b901e4311fec915853a0fb`. 124 JVM tests pass, zero failures/errors; lint zero errors and the existing orientation warning. Two parser tests, structure audit and whitespace checks pass.

Removes beta 49's two stacked tonal surfaces, obsolete section wrapper/style token and full-width selected-result information strip. One slimmer bounded-width field contains the shared icon submit control, with providers beside it and posters directly below on the existing gradient. Setup forms keep their regular field layout. Type badges/year/ratings remain with each poster; optional status/episode information or a synopsis excerpt occupies one supporting line, with matching space reserved on search cards without it. No new image loader, background process, API request type or dependency.

Final movie/series screenshots inspected: no stacked gray panels, integrated submit icon, complete result metadata and clear Movie/Series labels. Scoped dumps verify actual submission returns 24 results, lateral selection, Back to card zero, then query. Repeating the cached query refocuses card zero; Down reaches the next grid row and Up returns to zero. No media opened or configuration/account/Home-service changes in this pass. Search remains open for review; automatic input has stopped. TMDB live checks still require a user credential. No performance improvement is claimed from this visual simplification.

Private device artifacts stay under artifacts/search50. Docs updated; nothing committed or published.

---

# Local 1.0.1-beta.49 — installed, search surfaces and metadata checked

VersionCode 77. Optimized APK 1,775,746 bytes; installed SHA256 matches `d7f9c537da10ec3d1a62d5d95a17231caf32a3dfa022d63b8f68f8afb7619e2c`. 124 JVM tests pass, zero failures/errors; lint zero errors and one existing orientation warning. Structure, parser and whitespace checks pass. No dependency or manifest changes.

The reusable input now supports a leading icon and optional visible label; all fields share the same minimum-height token, and Search uses it for both the wide input and submit button. Shared non-interactive TV Material surfaces add tonal elevation behind related controls and focused-result information. The opaque artwork-color gradient remains the panel background. No live blur or per-poster shadow was introduced.

Search results have explicit Movie/Series/Anime badges; year and ratings remain beneath artwork. SIMKL's existing extended response supplies optional status/episode count; TMDB's response supplies a bounded synopsis. These populate a fixed-height summary whose selected-index subscription is isolated from the grid. Fixtures verify typed routing, status/episodes, ratings, synopsis and malformed/missing data without per-result detail fetches. TMDB remains unconfigured/off on this TV, so its live API and dual-provider selector are not claimed as tested.

Live SIMKL query returned 24 results. Final movie/series captures verify equal control heights, centered leading glyph, tonal grouping, readable type badges, complete first-row metadata and the selected series' “Ended · 85 episodes” information. Scoped dumps verify card 0 → card 1, Back to first result, then Back to the query with results still visible. A repeated cached query refocuses card zero; Down reaches the next grid row and Up returns to zero. Automatic input finished with Search open. No Nuvio/media launch, account/profile/permission/default-Home/DNS change or performance rebenchmark in this styling pass. Receipts/dumps/captures remain private under artifacts/search49; nothing committed or published.

---

# Local 1.0.1-beta.48 — installed, optional inline search checked

VersionCode 76. Optimized APK 1,775,746 bytes; installed SHA256 matches `b313a715d79e3bc6d8fd988bbe2cbd63c879b58b1063d4da794335cf1581b080`. 124 JVM tests pass (zero failures/errors), lint zero errors and the existing orientation warning. Two parser tests, shared-structure and whitespace checks pass. The first beta 48 R8 attempt encountered a temporary Windows DEX-file lock; retry succeeded without source changes or deletion. No new runtime dependency or manifest component.

SIMKL/TMDB are grouped under Settings → Integrations with independent Use in Search toggles, default Off. Provider choice requires enabled plus configured state. Real beta 46 dumps verified neither available before opt-in, then SIMKL available after the user-authorized toggle, while unconfigured TMDB stayed hidden. SIMKL search is now On for this TV; TMDB remains Off with no credential saved. Beta 47 verified Integrations/TMDB field/Back navigation and data credits, including the official TMDB mark, without saving a key. TMDB JSON mapping/auth setup is implemented and fixture-tested; no live TMDB search or both-configured selector has been certified without a user credential.

Final UI keeps query and results together. Live SIMKL extended search returned 24 mixed results; a targeted capture confirmed year/type and actual SIMKL/IMDb ratings beneath posters. Query entry shares MatrixTextEntry with setup forms, and search/list pages share MatrixMediaGrid. Native fit/focus/corners/card sizing remain shared. Optional metadata does not add height to ordinary cards. One request supplies ratings, with no per-result lookup; an eight-query/ten-minute memory cache avoids repeated requests. No query/token/credential appears in custom dumps.

Beta 47's strict checks verified Home row memory and Back/Home; final beta 48 checks verify whole settled Home slots and six centered toolbar frames. Inline Back goes from another result to card zero, then to the still-visible query/results screen. The equal cached-query test initially failed on beta 47 because data equality did not restart focus; beta 48 consumes an explicit completed-search event, and the previously failing sequence now passes.

Movie routing was visually verified in beta 46: The Batman (2022) opened its matching Nuvio detail page without pressing Play. Early series attempts were inconclusive: one capture still showed the previous movie, and another run observed #Cult playback. Input stopped; the user then authorized an idle-TV check. The final guarded check explicitly opened Matrix, asserted Search, waited for results and verified card index 1 remained selected before clicking. Its settled Nuvio capture shows Batman: The Animated Series (1992–1995), with Play S1 E1 untouched. Both movie and series title-level routes are therefore checked; package foreground alone and the earlier interrupted attempts are not counted as proof. Synthetic Home-key runs did not consistently bring Matrix to the foreground during these attempts; the final check used an explicit Home intent, so no fresh physical-Home certification or Home-service change is claimed. Existing accounts/Home permissions and DNS remain unchanged; no reboot or performance rebenchmark in this search pass.

Search/credits screenshots, scoped dumps and installation receipts remain ignored under artifacts/search46–search48. Nothing committed or published; stable GitHub release remains v1.0.0. See [search behavior and API references](../SEARCH.md).

---

# Local 1.0.1-beta.45 — installed, shared layout optimized

VersionCode 73. Optimized APK 1,757,126 bytes; installed SHA256 matches `cc423f179f61227a6ec90537d617bc43a0f41e406a96afdcfb753f0b2a2e1bd6`. Final build: 122 JVM tests, zero failures/errors; lint zero errors and one existing orientation warning. Two parser tests, shared-structure audit and whitespace checks pass. An initial Kotlin DSL scope error in the row-width change was caught by compilation and corrected before the installed build.

Shared artwork fits in the measurement phase instead of per-item subcomposition. Home passes its measured available width into favorites/channels, removing their repeated BoxWithConstraints while retaining the existing physical-pixel slot calculation. A snapshotFlow collector observes row focus outside Home composition and cancels obsolete top-return animations. No new dependency, cache expansion, animation-scale setting or accessibility change.

Strict active-instance dumps verify independent row memory (row 1/card 2, row 2/card 1), Back to first, then Home. Bounded fast/paced/synthetic-repeat navigation reaches all seven channels and returns Home. Settled rows show zero partial cards and five toolbar frames remain centered. Row geometry matches beta 43 exactly: favorites width 312/viewport 1720, channel 1 width 404/viewport 1744, portrait channel 2 width 253/viewport 1740 (physical pixels). Final white-app and portrait captures confirm artwork fit, rounded corners, halo and reserved spacing; both captures remain private.

Immediately preceding beta 43 stress baseline: 157 frames, 25.48% janky, median 38ms, p90 105ms, p95 150ms. Beta 44 first/warm: 27.22%/24.69% janky. Beta 45 first: 171 frames, 25.15% janky, median 42ms, p90 101ms, p95 200ms. Beta 45 warm: 194 frames, 20.62% janky, median 32ms, p90 69ms, p95 150ms. Same bounded command sequence, but actual focus paths/frame counts and loading conditions vary; this is encouraging warm-run evidence, not a controlled causal proof or 60fps result. Slow-tail jank remains. No explicit compilation request, playback, profile/account change or reboot in this pass. After automatic input stopped, the user tested favorites and held Down/release and reported: “Smoother and stops promptly.” This is physical feedback for this build, separate from injected checks.

Research and limitations are recorded in [MOTION-REVIEW.md](../MOTION-REVIEW.md). Receipts/dumps/captures stay ignored under artifacts/motion44 and artifacts/motion45. Nothing committed or published; GitHub stable remains v1.0.0.

---

# Local 1.0.1-beta.43 — installed, halo/loading and navigation checked

VersionCode 71. Optimized APK 1,757,122 bytes; installed SHA256 matches `c5ff3755ad94a677b8b3ee57b4f00b39c33fb910a5b7533000f9e1c404c320af`. Final build checks: 122 JVM tests, zero failures/errors; lint zero errors and one existing orientation warning. Two parser tests and the structure audit pass.

Preserves the exact 5.5dp artwork inset and 2dp gap. Removes the painted black outer keyline in favor of faint feathered gradient strokes. Blue TizenTube and white YouTube captures inspected: complete light rim, subtle outer halo, no painted black backing. Existing banner cache now seeds warm compositions directly; uncached media fades for 120ms. No new cache, dependency or live blur.

Strict resumed-instance dumps pass: row 1/card 2 and row 2/card 1 retain independent positions; Back resets row 2 to card zero, then returns Home/card zero. This caught a consumed Back request replaying after lazy-row reconstruction; beta 43 acknowledges and clears it. Full seven-row traversal, synthetic repeats, whole settled slots and five centered toolbar frames pass. System Settings opens, Home returns to Matrix, and six additional injected Home presses remain in Matrix. No media, profile, account, accessibility or reboot changes in this pass; injected keys do not certify a fresh physical-remote test.

First stress run: 158 frames, 32.91% janky, median 42ms, p95 200ms. Warm repeat: 175 frames, 22.86% janky, median 36ms, p95 150ms. Loading conditions differ; these samples do not establish a causal improvement against beta 39 or fully smooth motion. Worst-case focus/layout jank remains open. Experimental animated Home resets were removed after an early-input race; established immediate resets remain.

Device captures, test receipts and dumps remain ignored under artifacts/motion43. Documentation updated; nothing committed or published. GitHub stable remains v1.0.0.

---

# Local 1.0.1-beta.39 — installed, navigation and motion reviewed

VersionCode 67. Optimized APK 1,757,126 bytes; installed SHA256 matches `10f9995c4897cccd73d6073af0f424be3cc69430e5d10b0f17df29ef981095e5`. 122 JVM tests and two diagnostic-parser tests pass. Lint: zero errors, the existing orientation warning. Structure/whitespace and Python syntax checks pass. One initial diagnostic implementation hit RestrictedApi lint and was replaced with the supported existing Compose key handler, without suppression.

Independent active-instance dumps verify row focus memory, Back-to-first/Home, seven loaded rows, three selected custom SIMKL lists, connected Home capture/recovery, zero partial settled cards and centered toolbar frames. The physical remote test on beta 37's same repeat handler was reported clearer and stopping on release; its counters confirmed real repeat traffic, deferred repeats and settled scrolling. Later APKs were checked by injected input/dumps, not represented as fresh physical remote certification. No playback/profile/account/default-Home/reboot changes in this motion pass.

The failed constant-pivot experiment was removed from beta 38. Beta 39 retains explicit row focus groups, held-repeat backpressure, minimum-distance reveal, a viewport-relative adjacent-row cache and a shared lightweight gradient rim. One final screenshot verifies the rim; navigation tests use dumps only. Optional installer -Compile reports actual firmware compilation state rather than treating Success as proof.

Warm beta 39 stress sample: 163 frames, 25.77% janky, median 40ms, p90 81ms, p95 109ms. The sampled trace's mean focus-dispatch time fell from 17.664 to 14.807ms, but worst focus still took 85.775ms; disposal counts did not clearly improve. Cache-specific causation or uniformly smooth motion is not proven. Post-stress PSS snapshots differed by ~4MiB. See [MOTION-REVIEW.md](../MOTION-REVIEW.md) for experiment table, source guidance, trace evidence, compilation differences and limits. The earlier short 7.89% sample used another sequence and is not a direct baseline.

Private traces/screenshots/dumps stay ignored. Nothing committed/published in this motion pass; GitHub stable remains v1.0.0. Rendering jank remains an explicit follow-up.

---

# Local 1.0.1-beta.36 — installed and focus checked

VersionCode 64. Optimized APK 1,740,738 bytes; installed SHA256 matches `2853b6daf4b2c179a47ab08455ca6d9c53d2252df13a94401197a039f5442d26`. 121 tests pass, no failures/errors, zero lint errors and the existing orientation warning. Structure/whitespace checks pass.

Replaces beta 35's visually insufficient image-edge brightness border. Removed sampling/contrast code and its obsolete assertion, per-image edge state and forced software image loading. One shared artwork implementation reserves space inside TV Material's clipped image slot, then paints a separated 2dp outline with a 1dp contrasting keyline. Existing slot geometry and image-fit policy remain shared; artwork has a constant 6dp inset rather than changing size on focus. The added geometry test checks outline clearance and the existing focus gutter.

Targeted screenshots inspected on the TCL: blue TizenTube, white YouTube, white analiti, orange Downloader and a Nuvio media card show the complete outline. Logical dumps verify focus targets and zero partial slots in settled visible rows. First capture names did not match the actual focus index; subsequent explicit left/right navigation verified the intended blue/white targets before accepting the visual evidence. Captures remain private. Home helper remains connected and all three selected SIMKL custom rows remain available/visible. No new media playback, account change, reboot, physical-key retest or new performance measurement in this pass. Beta 35's measured jank remains unresolved; improved contrast is not evidence of smoother motion. No publication.

Research on optional add-on catalogs is in [ADDON-CATALOGS.md](../ADDON-CATALOGS.md); the current APK has no catalog-import feature. [Android TV focus outline guidance](https://developer.android.com/design/ui/tv/guides/styles/focus-system) and [AndroidX clipping implementation](https://github.com/androidx/androidx/blob/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/SurfaceImpl.kt) informed the shared outline geometry.

---

# Local 1.0.1-beta.35 — installed and device checked

121 tests pass, zero failures/errors/skips. Focused geometry, focus-contrast, safe launch-flag and existing feed/order regressions also passed separately. Structure audit and whitespace checks passed. Full optimized build succeeded; lint has zero errors and the existing DiscouragedApi orientation warning. APK: 1,740,738 bytes, SHA256 `fe302c91e39b79e10825f44500ad53d09dfdbc81b13c26ab359417a8c58971dc`. No runtime smoothness improvement is claimed before a matched device check.

Installed with explicit approval; installed SHA256 matches the optimized artifact. Home capture/recovery remain connected/enabled. The white YouTube artwork has a dark inset focus outline; toolbar frame/icon offsets are zero. XML hierarchy inspection returned a null root on this TCL, so logical activity/window dumps were used for navigation and targeted screenshots for artwork, list selection and Nuvio destinations.

SIMKL: account connected, Home enabled, three pins available but initially only two visible. Re-saved the same existing selections through Save lists; all three became visible without changing the selected IDs. Confirmed custom-list posters on Home. At the seven-item preview's end, visible slots are 1–6 with zero partial cards; Back restores card zero and slots 0–5, then a second Back returns to favorites/card zero.

Installed Nuvio is 1.1.0-beta.10. Provider routes publish flags 0x14000000, content extras and stream mode. Shell queries cannot see the app's channel rows, so an empty shell provider result is not evidence of absent channels. Opened Nuvio, returned Home and selected Continue Watching. After the user selected the profile, the intended title reached its Starting stream screen. Returned Home and selected a different card while Nuvio was running; the second intended title reached its Starting stream screen rather than restoring the prior title. This verifies warm content routing; successful playback, exact resume position and network/provider health were not certified.

Bounded navigation sample: 266 rendered frames, 21 janky (7.89%), median 29 ms, p90 40 ms, p95 46 ms, p99 113 ms. There is still measurable jank. This is not a matched baseline comparison or proof of uniformly smooth motion; no FPS improvement is claimed. GPU percentile sentinel values were excluded. Returned the TV to Matrix Home. Temporary TV capture files removed; private screenshots/dumps remain ignored locally. Nothing published; v1.0.0 remains the GitHub stable release.

Review references: [Compose performance](https://developer.android.com/develop/ui/compose/performance/bestpractices), [Android task routing](https://developer.android.com/guide/components/activities/tasks-and-back-stack), [Nuvio warm launch handling](https://github.com/NuvioMedia/NuvioTV/blob/dev/app/src/main/java/com/nuvio/tv/MainActivity.kt), [SIMKL custom lists](https://api.simkl.org/guides/custom-lists). Upstream dev source is supporting evidence, not a byte-for-byte match of the installed Nuvio build.

---

# Stable Compose v1.0.0

VersionCode 62, optimized APK 1,740,726 bytes. SHA256 `5340a38005214c18c3a2c812fa459ec6ed24300b580c3ac95c0f1e259633a62a`. Release: v1.0.0 in the then-current repo.

Version/metadata promotion of verified beta 34, with no feature changes. 119 tests pass; zero lint errors and one existing orientation warning. Installed update and exact APK hash verified. Nuvio remained foreground throughout installation. Compose Home control remains enabled/bound; no stale accessibility shortcut targets were set. The old Java app remains available and its helper entries remain off by user choice. TCL rejected per-component hiding, so no old service/package was disabled. No reboot or physical Home test was repeated for the version-only update.

The beta 34 source/UI audit and integration limits below still apply.

---

# Local beta 34 whole-project review

Version 1.0.1-beta.34, code 61; optimized APK 1,740,738 bytes. Installed hash matches `4dc530ef8a92e08c8e16f35c9d83ca84c6527d4797d9a050a281599dec6d3429`. Release: v1.0.1-beta.34 in the then-current repo, a prerelease.

Three subagents reviewed UI, data/core and platform source/tests file by file; the parent reviewed build/test tools. See PROJECT-REVIEW.md, AUDIT-DATA.md and AUDIT-PLATFORM.md for coverage and concrete findings. Production changes total 111 lines added /263 removed (net 152 removed). Removed only unused implementations/data paths and their obsolete tests. Native TV Material cards/buttons/surfaces remain; shared progress now uses theme roles and accessibility semantics. No runtime dependency was added.

119 JVM tests passed, zero failures/errors, zero lint errors and only the existing orientation warning. Focused mixed app/core filters were run successfully, confirming the build-script correction executes both modules. Python tools compile and structure/whitespace checks pass. Windows transiently locked classes.dex during the final R8 pass; the retry succeeded without source changes.

Device verification on TCL: exact optimized bytes installed; Home capture connected, recovery retained, 27 app entries/seven channel rows loaded. Initial beta 33 first-Down screenshot exposed a stale focus-group read suppressing reveal; beta 34 reads current focus state directly in the scroll policy. Final first Down fully reveals the selected media card, Up returns the top/header context. Targeted progress screenshot shows the rounded themed track/indicator inside artwork and the retained single rounded focus rim. Whole-row diagnostics report zero partial slots; five toolbar icon frames remain centered. Browsed Wallpapers/source and Back without saving selections. TV Settings opened com.android.tv.settings.MainSettings and injected Home returned to Matrix. No new media playback, account authorization, source/profile change or permission/default-Home change was performed.

XML hierarchy capture on this TCL can suppress the selected accessibility helper, so this pass used scoped logical activity/window dumps and final visual inspection. Progress accessibility semantics were reviewed in source, not certified by a full screen-reader session. No reboot or physical remote test was repeated because routing/capture policy did not change. Fresh authorization, active-import cancellation, all provider cursor variants and every vendor fallback remain integration limits. The global SIMKL pagination disk-cache retention cap remains a documented follow-up. No FPS or whole-app RAM improvement is claimed.


Earlier build evidence is retained in [validation history](../archive/VALIDATION-HISTORY.md).
