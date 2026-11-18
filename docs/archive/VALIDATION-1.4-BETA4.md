# 1.4.0-beta.4 preview validation — 2026-11-04

Installed on TCL C755 / Android 14; **not published**. VersionCode 98. Optimized APK **1,793,630 bytes** (1.71 MiB). Installed SHA256 matches `7b2cb8e92c7872d29a4eb2272c46fbc23f69d7cd6965a6e3b6c715755a0c5bcb`.

Final Checks build: 74.8s, **148 JVM tests**, zero lint errors and the existing orientation warning. Structure audit, two tool tests and whitespace checks pass. [Scope and sources](RELEASE-1.4.md).

## Revolving coloured border

The requested effect is now a cyan/blue/violet/pink gradient revolving around the full focused card/control border, one revolution per four seconds. It replaces the previous passing white highlight. Unfocused items do not animate; the rotation coroutine pauses outside the resumed activity lifecycle. Clear surfaces, control geometry and navigation remain unchanged. Shader, paint and matrix are cached; angle reads occur in drawing only. This intentionally produces continuous drawing while a visible item is focused.

A TV recording and frame strips show the colour bands changing position around a card and a top icon. Scoped activity dumps verify favorites indices 0→1→0, header entry and Back to Home. Icon-center and settled whole-slot checks pass. Playback-free Android Settings entry/return verifies lifecycle navigation. Accounts, favorites, channels, wallpaper preferences, Home capture/recovery and service identities remain unchanged. Automatic input stopped on Home.

## Performance sample

Same 24-move warm first-channel scenario as earlier previews: **998 frames, 12 janky (1.20%), p95/p99 46/46ms**. The previous finite-sweep preview recorded 461 frames, 24 janky (5.21%), p95/p99 46/46ms. The continuously moving border more than doubles rendered frames in this sample; the percentage is not an equivalent smoothness comparison. Changing automatic wallpaper and small sample counts remain confounders. This is a cosmetic change, not a fix for composition/layout or render-buffer waits. Vendor GPU counters remain unreliable.

Ignored evidence: `artifacts/profiling/revolving-border.mp4`, `revolving-*-strip.png`, `revolving-after*`, `beta4-*.txt`. Earlier baseline/trace/navigation evidence: [beta 3](VALIDATION-1.4-BETA3.md), [beta 2 and original 1.3 comparison](VALIDATION-1.4-BETA2.md). Palette-backdrop transitions still need a controlled scenario. Home key filtering and its held-volume tradeoff are unchanged. No publication.
