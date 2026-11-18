# Historical changelog

## 1.1.1-beta.7 — prerelease

Includes the local beta 1–6 changes below. [Release notes](RELEASE-1.1.1-beta.7.md).

- Replace the remote-inaccessible native wallpaper picker with shared TV album menus and image cards.
- Add contextual image-only access with Android 14 partial-access handling; no all-files/video/write permission. Preserve file-manager sharing and existing backgrounds.

## 1.1.1-beta.6 — local wallpaper picking

- Use the system image picker instead of the document stub on this TCL. Add user-confirmed image Open with/Share import without broad storage permission.
- Add a shared-card gallery for choosing saved wallpapers. Bound imports and preserve the selected local image during retention. Serialize wallpaper storage across Activities.
- Validate image MIME/content URI scope and reject paths, remote links and private provider payloads. Reuse existing sampled decode and JPEG storage.
- Keep regular search unchanged; SIMKL AI mode deferred after documentation review and user choice. YouTube changes skipped. Included in the beta 7 prerelease and stable 1.2.0.

## 1.1.1-beta.5 — local Matrix namespace

- Move app implementation/tests and Android resources to com.matrix.launcher; update current tool/install/test paths and use Theme.Matrix.
- Retain four minimal legacy service bridges and a single Home activity-alias to preserve installed accessibility/default/shortcut identities. Keep application ID, accounts and preference keys stable.
- Accept old configured remote actions while using Matrix actions for new calls. Strengthen qualified manifest/alias/bridge checks and compatibility regression coverage.
- Preserve upstream MIT/font/icon notices and clarify branding versus attribution. Included in the beta 7 prerelease and stable 1.2.0.

## 1.1.1-beta.4 — local settings consistency

- Remove the isolated Reload Home leading icon and its unused optional settings-icon path. Use the same SettingAction/SettingsPage route as neighboring settings; retain stable reload identity and loading behavior.
- Define standalone action-label typography once in MatrixButton; compact Save rows inherit it. Preserve the shared settings title/detail hierarchy, toolbar icons and explicit commit/destructive cues.
- No integration, account or Home-helper behavior change. Included in the beta 7 prerelease and stable 1.2.0.

## 1.1.1-beta.3 — local configurable media app

- Separate catalog identities from Nuvio; add Settings → Integrations → Open titles with, with compatible installed Nuvio/Stremio and automatic selection.
- Retain publisher-specific Continue Watching intents, native episode/resume extras and Nuvio warm-detail compatibility. Use Stremio's distinct CLEAR_TOP recreation contract instead of SINGLE_TOP reuse.
- Retain SIMKL source references for a cached on-click IMDb lookup; optionally map TMDB-only titles with the existing TMDB credential. Reuse existing HTTP/cache handling and cancel late media delivery after navigation/stop.
- Document portable Android TV channels, ID limits and future adapter requirements. No new dependency or permission. Included in the beta 7 prerelease and stable 1.2.0.

## 1.1.0 — stable Compose release, 2026-11-04

- Include the tested search, shared TV Material controls, card/navigation and optional integration improvements from the 1.0.1 beta series.
- Fix warm Nuvio title switching globally for Search, SIMKL and published detail cards; preserve native Continue Watching context and ordinary app resume.
- Move remote configuration reads off main with lifecycle cancellation. Bound and atomically replace SIMKL cache snapshots while protecting metadata/library/pinned previews.
- Reuse row derivations and artwork aspect values; keep default nested prefetch after rejecting a regressing experiment.
- Finalize release/setup documentation, retain measured limitations and archive local beta notes. No new runtime dependency or permission.

133 JVM tests; zero lint errors. See [release notes](RELEASE-1.1.md), [validation](../VALIDATION.md) and [historical beta experiments](../archive/BETA-54-56.md).

## 1.0.1-beta.53 — component audit prerelease, 2026-11-04

- Fix refresh cancellation/loading and app-catalogue invalidation races; handle vendor Inputs failures.
- Reuse remote launch handling and SIMKL valid-item mapping; skip malformed records and invalid library IDs before pagination.
- Replace no-op credit rows with status pages and defer license focus until layout. Correct stale smoke routes and isolate synthetic Home routing from scroll tests.
- Add deterministic 10,000-case spacing/reveal coverage and malformed provider fixtures. No new dependency, permission, account change or media launch.

Published as a GitHub prerelease with the exact optimized APK installed and verified on the TV. Validation and remaining limits are recorded in docs/VALIDATION.md.

## 1.0.1-beta.52 — local single-row Search and request cleanup, 2026-11-04

- Put Back, query, submit and separate Voice icons on one top row; remove the repeated Home breadcrumb/Search title and the extra form level.
- Replace submitted-query/request/completion counter state with one cancellable composition-scoped job. Cached submissions still explicitly restore first-result focus.
- Return cards directly from search instead of constructing an unused MediaRow title/ID wrapper. Remove a stale repository JSON import. Retain actual manifest components, callbacks and shared controls.

Installed and exact hash verified. All 124 JVM tests and two parser tests pass; no lint errors or new warnings. Scoped dumps confirm typed and cached submissions, grid navigation and two-stage Back. Nine icon frames are centered within 0.5 physical pixels; the final single-row screenshot was inspected. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.51 — local text/voice separation, 2026-11-04

- Replace the embedded submit orb with clearly labeled Search and Voice actions; keep query and posters on one page. A single provider lives in the field hint instead of a redundant adjacent label.
- Delegate voice to the available system recognition activity; no recording permission, background service or new library. Accepted speech uses the existing search/cache path; cancellation preserves the current page.
- Remove the unused trailing-action field API, extra layout Row and unused imports. Hide Remove TMDB credential when no credential exists.

Installed and exact hash verified. 124 JVM tests and two parser tests pass; no lint errors or new warnings. Typed submission, cached-query refocus, grid navigation and two-stage Back pass. Final screenshot inspected; voice provider resolution confirmed without activating capture. The user confirmed real-remote voice recognition returns matching results. Live TMDB remains pending. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.50 — local search simplification, 2026-11-04

- Remove both stacked gray surfaces, their now-unused wrapper/style token and the repeated full-width result information panel.
- Keep one slimmer bounded-width input with its submit icon integrated; place provider choices beside it and posters directly below on the ambient gradient.
- Retain type/year/ratings and concise supporting information with each poster. Preserve explicit submission, cached-query focus, Back behavior and optional integrations.

Installed and exact hash verified. 124 JVM tests and two parser tests pass, zero lint errors. Final movie/series captures inspected; integrated submit, two-stage Back, cached query refocus and vertical grid navigation pass. Search left open; no media launched. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.49 — local search presentation preview, 2026-11-04

- Align the wide search field and submit button through a shared height token; add the leading search glyph to the reusable input. Use shared non-focusable TV Material tonal surfaces to group query controls and focused-result information over the existing opaque artwork gradient.
- Add distinct Movie/Series/Anime badges, with year/ratings below the poster. Reuse available SIMKL status/episode count and TMDB overview in a fixed-height focused information strip; no extra detail requests or runtime dependency.
- Keep selected-card observation inside the summary component, preserving the grid geometry, submitted-query cache, Back behavior and provider gating.

Installed and exact hash verified. 124 JVM tests and two parser tests pass, zero lint errors. Live SIMKL results/screenshots verify aligned controls, type badges, complete first-row metadata and focused status/episode count. Cached resubmission, two-stage Back and vertical result navigation pass. TMDB synopsis mapping is fixture-tested; live TMDB remains pending a user credential. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.48 — local inline search correction, 2026-11-04

- Treat completed searches as focus events, even when an identical cached result compares equal. Resubmitting a query can focus the first result without relying on data inequality.
- Retain the inline query/results layout, shared form field/media grid, optional integrations and provider rating metadata.

Installed and exact hash verified. 124 JVM tests and two parser tests pass, zero lint errors. Inline Back-to-first/query, equal cached-query refocus, live extended ratings, matching movie/series Nuvio details, whole-card Home bounds and six centered toolbar frames checked. TMDB remains off pending a user credential; its live API and dual-provider selector are not certified. Nothing published.

## 1.0.1-beta.47 — local inline search preview, 2026-11-04

- Keep query, enabled-provider controls and results on one screen. Reuse one extracted text entry for setup/search and one media grid for lists/search; remove the separate search-form/results branches and temporary form customization.
- Add year/type and available provider ratings under shared result artwork. SIMKL uses extended search in one request; TMDB uses multi-search metadata, filtering people and invalid IDs. No per-result API lookup.
- Retain optional integration toggles, bounded submitted-query caching, Nuvio title links, data credits and softer cached focus halo from beta 46.

Installed and exact hash verified. 124 tests pass, zero lint errors. Live extended SIMKL search displays 24 results with year/type and real ratings, query remains visible, two-stage Back works, Home memory and six toolbar frames pass. Integrations/TMDB setup/credits navigation checked without saving a TMDB credential. Superseded by beta 48 after the repeated cached-query check caught focus staying on Search. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.46 — local optional search preview, 2026-11-04

- Add the shared-toolbar Search icon and optional SIMKL/TMDB providers, each gated by setup and an explicit Use in Search toggle. Group optional service setup under Settings → Integrations. Both toggles default Off.
- Reuse SIMKL's auth/quota client, bound query cache to eight entries/ten minutes, perform IO off-main only on submit, and launch result details in Nuvio. Include TMDB attribution and its approved logo.
- Soften the existing shared edge halo without changing spacing or adding blur; cache stroke objects outside animation draws.

Installed and exact hash verified. 124 JVM tests and two parser tests pass, zero lint errors. Device checks verified both-disabled setup, SIMKL opt-in, TMDB hidden while unconfigured, 24 live results, Back-to-first/query, repeated query and correct The Batman (2022) Nuvio details without playback. Superseded by beta 47's inline layout and richer result metadata. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.45 — local layout performance preview, 2026-11-04

- Reuse Home's measured width for favorites and channel rows; remove their redundant BoxWithConstraints subcomposition while preserving the pixel-based whole-card fit.
- Retain beta 44's direct artwork measurement and composition-independent focus observer, existing glow/spacing, row memory and immediate Home/Back behavior.

Installed and exact hash verified. 122 JVM tests and two parser tests pass; zero lint errors. Strict row-memory/Back, whole-card geometry and centered toolbar checks pass; app/portrait screenshots inspected. Warm stress measured 20.62% janky frames, median 32ms and p90 69ms versus the preceding beta 43 baseline's 25.48%, 38ms and 105ms. p95 remains 150ms; loading/input conditions vary, so fully smooth motion and causal gains are not established. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.44 — local layout performance preview, 2026-11-04

- Replace per-card BoxWithConstraints with one-pass fitted artwork measurement in the shared layout modifier; remove one wrapper.
- Observe row focus inside a cancellable snapshotFlow collector, avoiding whole-Home recomposition just to restart its top-return effect.

Installed and exact hash verified. 122 tests pass, zero lint errors. Strict row-memory/Back, Home traversal, whole-card and icon-frame checks pass. Warm stress measured 24.69% janky frames versus the preceding beta 43 baseline's 25.48%, insufficient to establish a reliable improvement. Superseded by beta 45 for additional row measurement reuse. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.43 — local glow/loading preview, 2026-11-04

- Clear a consumed Back-to-first-card request so recreating a lazy row cannot replay it and steal Home focus.
- Retain the soft halo, previous spacing, immediate warm-banner cache hits and uncached poster fade. Keep established fast Home reset behavior.

Installed and exact hash verified. 122 JVM tests and two parser tests pass; zero lint errors. Strict row-memory/Back, whole-card geometry, centered icons, Settings return and repeated Home checks pass. Blue/white artwork captures inspected. Rendering jank remains; see docs/MOTION-REVIEW.md. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.42 — local glow/loading preview, 2026-11-04

- Keep the soft tinted halo, exact previous artwork spacing, shared warm-banner seeding and uncached poster fade.
- Restore beta 39's proven Home/Back reset implementation after stricter device dumps caught a regression in the attempted consolidation. No new Home-capture behavior.

Superseded by beta 43: restoring the reset implementation alone did not fix replay of a consumed row Back request. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.41 — local glow/loading correction, 2026-11-04

- Retain beta 40's feathered halo, unchanged artwork spacing, shared memory-seeded banners and uncached poster fade.
- Remove the animated Home/Back resets after a dump check exposed a late-focus race. Preserve fast reset semantics and the consolidated cancellable reset effect.

Superseded by beta 42 after the strict Back-to-Home device check failed. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.40 — local glow/loading preview, 2026-11-04

- Preserve the existing 5.5dp artwork inset and 2dp artwork-to-rim gap; remove the dark keyline. Two low-opacity tinted light strokes feather outside the shared gradient rim without live blur or new bitmap generation.
- Seed app and mini artwork from the existing memory cache through one shared loader, avoiding a fallback frame on a warm composition. Uncached media posters use a 120ms crossfade; Coil skips memory-cache transitions.
- Consolidate explicit Home/Back-to-Home resets in one cancellable effect and animate row/dock return instead of snapping. First layout remains immediate; app-return position and Home capture policy are retained.

Installed for testing; superseded by beta 41 after an early-Down/Home-reset race was observed. Remaining worst-case focus/layout stalls from the prior trace are not claimed solved. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.39 — local motion experiment, 2026-11-04

- Add a viewport-relative Home cache window (half a viewport ahead/behind) to reduce rebuilding adjacent rows; retain existing bounded artwork caches. No new library.
- Optional installer `-Compile` requests app-only ART precompilation before launch and reports the actual filter. APK hash validation remains mandatory; this does not change APK bytes or system animation scales.

Installed and hash verified; 122 JVM tests and two parser tests pass, no lint errors. Dump checks cover row-memory, Back/Home and whole-card/icon geometry; one final focus-style capture inspected. Trace comparison shows lower mean focus time but ~86ms worst-case stalls remain; no fully smooth/60fps claim. See docs/MOTION-REVIEW.md. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.38 — local motion experiment, 2026-11-04

- Remove beta 37's unsuccessful 20% vertical alignment policy and its obsolete test. Retain minimum-distance reveal, explicit row focus groups, held-repeat backpressure and the shared lightweight focus sheen.
- Keep the dump-first test harness and resumed-instance diagnostics; no frame-rate claim without measured verification.

Installed and verified, then superseded by beta 39. Navigation checks passed; rendering jank remained. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.37 — local motion experiment, 2026-11-04

- Consistent clamped vertical focus line, explicit horizontal focus groups and backpressure only for held vertical repeats while a reveal is moving. Preserve fresh presses, native reveal/scale and Home/Back behavior.
- Thinner contrasting keyline and cached gradient focus sheen; no blur, extra image sampling or per-frame bitmap generation.
- Add vertical scroll/repeat diagnostics, a bounded dump-only navigation harness and active-instance parsing. Remove the stale top-group state wrapper. Existing measurement screenshots become opt-in.

Installed and hash verified. 123 tests passed with zero lint errors; stress navigation/geometry worked but jank measured 31–32%, so the always-align policy is superseded by beta 38. Not published.

## 1.0.1-beta.36 — local preview, 2026-11-04

- Replace beta 35's insufficient inset dark/light border with one shared separated outline and contrasting keyline around fitted artwork. Artwork clipping and focus gutter remain independent of the outline.
- Remove edge brightness sampling, contrast math, their obsolete test, extra per-image state and forced software image requests. Add a focused geometry check that outline/scale stay within the existing gutter.
- Document generic add-on catalog feasibility, reuse and compatibility boundaries; no catalog feature or new dependency has been added yet.

Installed and exact hash verified. Blue/white app tiles and media artwork visually checked; settled rows have zero partial cards and all three selected SIMKL rows remain visible. 121 tests pass; no lint errors. Motion jank remains a known follow-up; this build changes contrast, not a measured performance outcome. Included in the beta 7 prerelease and stable 1.2.0.

## 1.0.1-beta.35 — local preview, 2026-11-04

- Artwork-aware light/dark focus outlines sampled from image edges, keeping one inset rounded stroke.
- Focused media sampling no longer invalidates the Home composition; event-only card positions no longer trigger horizontal-focus recompositions.
- Preserve safe publisher CLEAR_TOP/SINGLE_TOP launch flags. Nuvio detail extras gain its public meta URI for warm navigation; public URI launches stay in the source app's task.
- Read selected SIMKL custom list IDs directly, independently of the index page. Saving chosen SIMKL rows clears their old hidden-row flags.
- Added account/list-count and launch-shape diagnostics without titles, IDs, links or credentials.

Installed and exact hash verified. White-artwork outline, three selected SIMKL rows, end-of-row/Back navigation and two Nuvio content destinations (including a warm task) checked. 121 tests pass, zero lint errors. Short navigation sampling still reports 7.89% janky frames; smoothness remains a follow-up. Unpublished; existing stable release remains v1.0.0.

## 1.0.0 Compose — 2026-11-03

First stable Compose release, based on the verified beta 34 audit. The version name starts at v1.0.0; versionCode increases to 62 for in-place updates from all earlier previews. Future stable patches use 1.0.1 and later.

## 1.0.1-beta.34 — 2026-11-03

This prerelease contains the independently built Compose launcher and the latest verified cleanup/navigation fixes.

- TV Material 3 cards and controls with shared rounded progress rendering and accessibility semantics.
- Whole-card row spacing, preserved portrait sizing, corrected single focus rim and first-Down reveal.
- Optional Commons landscapes and local wallpapers, offline rotation and saved attribution/source QR links. Reddit removed; existing images retained.
- Dead caches/helpers/metadata paths removed; SIMKL writes run on IO under the shared account lock; banner invalidation and provider portrait metadata corrected.
- Updated setup, rollback, testing and file-by-file audit documentation.

Validation: 119 JVM tests pass; zero lint errors, one existing TV-orientation warning. Optimized APK is 1,740,738 bytes. Installed APK hash matched on the TCL; targeted card/progress, navigation, settings and row/icon checks passed. This does not certify every provider/account/vendor combination. See docs/VALIDATION.md and docs/PROJECT-REVIEW.md.

Known limits: TCL key capture can reduce held-volume repeats; source apps control published channels/profile refresh; SIMKL has quota limits and no global pagination-cache cap. Helpers/recovery remain opt-in for new installs. No account keys or device artifacts are included.

SHA256: 4dc530ef8a92e08c8e16f35c9d83ca84c6527d4797d9a050a281599dec6d3429


## Earlier previews

The previous GitHub prerelease was beta 9. Intermediate local previews and their device evidence are retained in [validation history](../archive/VALIDATION-HISTORY.md). Current setup instructions are in the README and docs/HOME-SETUP.md.
