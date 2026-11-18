# 1.4.0-beta.3 preview validation — 2026-11-04

Installed on TCL C755 / Android 14; **not published**. VersionCode 97. Optimized APK **1,793,634 bytes** (1.71 MiB). Installed SHA256 matches `f62626ce1a3a05fcc1fa9c88b6f8fc7ee0e446e34f0e487e1003245cc39faa52`.

Final Checks build: 84.1s, **148 JVM tests**, zero lint errors and the existing orientation warning. Structure audit and whitespace checks pass. [Scope and design sources](RELEASE-1.4.md).

## Focus-border motion

A 600ms moving light gradient overlays the existing card/control rim once when focus arrives. It settles without an infinite loop, live blur, new colors, geometry changes or navigation changes. Shared shader, paint and matrix objects are cached; animation values are read only in drawing. Clear fills remain intact. TV recording and extracted frames verify card/top-control transitions; scoped dumps verify favorites indices 0→1→0, header entry and Back to Home. Icon centering and settled whole-slot checks pass. After settling, a two-second idle sample recorded zero rendered frames, confirming the effect stops.

## Immediate before/after sample

Same 24-move warm first-channel scenario, optimized beta 2 before and beta 3 after, no forced compilation or cache clearing. Wallpaper rotated again, so this is not an image-identical benchmark.

| Metric | Beta 2 before | Beta 3 after |
| --- | --- | --- |
| Frames | 382 | 461 |
| Janky frames | 24 (6.28%) | 24 (5.21%) |
| p95 / p99 | 38 / 46ms | 46 / 46ms |

**This is not a smoothness improvement.** The longer finite effect produces more frames; the lower jank percentage has a larger denominator, while the same 24 frames miss deadlines. p95 is worse in this sample. The effect adds drawing work and does not fix the existing composition/layout and render-buffer waits. Prior source-level tracing did not identify the exact expensive composable. Vendor GPU percentiles remain unreliable.

The original 1.3 baseline, repeated beta 2 comparison, row/app-return checks and trace limitations remain in [earlier preview evidence](VALIDATION-1.4-BETA2.md). Palette-backdrop transitions still need a controlled scenario; no claim of smooth 60fps or a measured memory improvement.

Ignored evidence: `artifacts/profiling/glass-sweep-before*`, `glass-sweep-after*`, `glass-sweep.mp4`, `sweep-frames/`, `beta3-*.txt`. Accounts, favorites, channels, wallpaper choices, Home capture/recovery, compatibility identities and the held-volume tradeoff remain unchanged. Automatic input stopped on Home. No publication.
