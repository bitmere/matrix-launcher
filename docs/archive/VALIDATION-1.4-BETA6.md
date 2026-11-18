# 1.4.0-beta.6 preview validation — 2026-11-04

Installed on TCL C755 / Android 14; **not published**. VersionCode 100. Optimized APK **1,793,634 bytes** (1.71 MiB). Installed SHA256 matches `af1f29fd512997b40f8008c47d7bd3e6d323d0c2a1e46c2bdbdb35b84d5773cf`.

Final Checks build: 75.3s, **148 JVM tests**, zero lint errors and the existing orientation warning. Structure audit, two tool tests and whitespace checks pass. The updated layout regression checks the expanded glow's actual painted footprint, focus-scale gutter and minimum usable artwork. [Scope and primary sources](../RELEASE-1.4.md).

## Artwork-coloured outward glow

The rotating gradient now spreads outward in soft flame-like plumes. Fixed rainbow colours are removed. App cards use their own cached artwork accent; media cards use the existing sampled artwork palette. Nuvio orange, TizenTube cyan, YouTube red and a red media card were visually verified. Control glow follows the active palette. No extra image decoding or live background blur.

The artwork inset reserves the complete 8dp plume spread inside unchanged card slots; artwork is slightly smaller to accommodate it. Android 13+ draws the glow with one bounded runtime-shader pass plus the existing rim; older versions retain the stroke fallback. The older-device fallback was not device-tested. Rotation runs only for focused items while the activity is resumed.

A TV recording and extracted frames verify outward spread, colour changes and clipping. Scoped dumps pass favorites indices 0→1→0, header entry and Back to Home. Six icon frames are centered within 0.5 physical pixels; settled rows have whole visible slots. Background/return checks passed, with zero frames rendered in a two-second sample while Android Settings was foreground. No shader fallback or runtime error appeared in the checked logs.

## Same navigation scenario

24 paced Right/Left moves in the warm first channel, optimized builds on the same TV:

| Renderer | Frames / janky | p95 / p99 |
| --- | --- | --- |
| Border-only beta 4 | 952 / 2 (0.21%) | 44 / 46ms |
| Initial outward-glow beta 5 | 951 / 49 (5.15%) | 53 / 61ms |
| Single-pass outward-glow beta 6 | 960 / 12 (1.25%) | 46 / 46ms |

The single-pass version reduced the first glow implementation's observed cost. It still adds cost versus the border-only sample. This is a visual enhancement, not a claim that Home jank is fixed. Automatic wallpaper changes and single-sample variation limit causal conclusions; vendor GPU percentiles are unreliable. Composition/layout and buffer waits remain investigation targets.

Ignored evidence: `artifacts/profiling/artwork-plume-final.mp4`, `plume-final-*.png`, `plume-before*`, `plume-after*`, `plume-final-after*`, `beta6-*.txt`. [Earlier border validation](VALIDATION-1.4-BETA4.md) links the original baseline and prior trace evidence. Accounts, favorites, channels, wallpaper preferences, Home capture/recovery, service identities and held-volume tradeoff remain unchanged. Automatic input stopped on Home. No publication.
