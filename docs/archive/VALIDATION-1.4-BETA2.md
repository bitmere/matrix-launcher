# 1.4.0-beta.2 preview validation — 2026-11-04

Installed on TCL C755 / Android 14; **not published**. Stable 1.3.0 remains published. VersionCode 96; optimized APK **1,793,634 bytes** (1.71 MiB, 8 bytes above 1.3). SHA256 `fffd78ee07c5ba8052608ec0ebd250d64ccecaf15dd7a3785990963dffd50577`; installer verified exact installed bytes.

Final Checks build: 80.1s, **148 JVM tests**, zero lint errors and the existing orientation warning. Structure audit, two tool tests, Python syntax, changed-document links and whitespace checks pass. [Preview scope and primary design sources](../RELEASE-1.4.md).

## Performance comparison

New baseline recorded on installed optimized 1.3.0 before performance edits. Same TV, row, 24 paced Right/Left moves and measurement commands; two samples per version. No force-stop, cache clearing, preference changes or additional ART compilation. Automatic wallpaper rotation changed the photo between versions; these are not tightly controlled image-identical benchmarks.

| Scenario | 1.3 baseline | 1.4 beta 2 |
| --- | --- | --- |
| Warm row, sample 1 | 24/384 janky (6.25%); p95 40ms | 26/381 (6.82%); p95 46ms |
| Warm row, sample 2 | 25/379 (6.60%); p95 38ms | 27/384 (7.03%); p95 42ms |
| Dock, six moves | 6/102 (5.88%); p95 36ms | 6/102 (5.88%); p95 38ms |
| Seven Down + seven Up | 27/240 (11.25%); p95 57ms | 30/224 (13.39%); p95 53ms |
| Return from Android Settings | 7/40 (17.50%); p95 200ms | 7/34 (20.59%); p95 200ms |

**No measured smoothness improvement.** Warm-row p95 and jank are worse in these samples. Small sample counts, process warm-up and changing wallpaper prevent attributing the difference to a particular edit. Return is a single playback-free external-app sample, not a playback-return certification. Do not present vendor 4950ms GPU percentile values as meaningful measurements.

Perfetto captures cover part of each warm-row run. Baseline worst missed frame was 37.30ms, including 17.82ms main doFrame, 8.41ms recomposition and 5.57ms measure/layout (nested times). Composition/layout and render-buffer waits remain; source-level composition tracing is absent, so no exact composable is established as the cause. Only one texture upload was observed in the baseline, not evidence for rewriting artwork loading. Unsupported ftrace events limit coverage. Active photo wallpaper means palette-backdrop transitions were reviewed in code but not measured; they still need a controlled scenario. No lower-quality backdrop, cache replacement or return-refresh removal was attempted.

Raw traces, SQL, gfx reports, screenshots and the repeated scenario script remain in ignored `artifacts/profiling/` (1.4-before*, 1.4-beta2* and 1.4-trace-analysis.md). This establishes evidence for further work; it does not certify smooth 60fps.

## Navigation, appearance and retained state

Final beta 2: scoped resumed-activity dumps verify Home, seven-row traversal, return from Android Settings, Edit favorites entry/cancel, Home channels entry, restored Settings focus and Back to Home. Six Home icon frames are centered within 0.5 physical pixels; settled visible rows have no partial slots. Top controls, dock, focused artwork and Settings screenshots were inspected. Clear surfaces retain visible wallpaper; no live blur or dark opaque Home icon fill.

XML smoke recorded Home but failed to find Settings because TCL returned the underlying Home hierarchy. This was **not counted as an XML pass**: resumed Settings state and its screenshot verified the actual page, followed by scoped D-pad checks. Earlier beta 1 burst/hold stress reached all seven rows; final beta 2 did not repeat physical remote testing.

Accounts remain connected, three custom lists/seven rows and Automatic media routing retained. Accessibility-service selections match exactly; Home connected/recovery remain true. No favorites, channels, wallpaper preference, account, integration or Home mode was changed. Automatic wallpaper rotation continues. Home key filtering and its held-volume repeat tradeoff are unchanged. No app uninstall, data clear or publication. Automatic input stopped on Home.

[Stable 1.2/1.3 evidence](VALIDATION-1.2-1.3.md).
