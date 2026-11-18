# Validation

## Current validation: awaiting overseer run

Source baseline: 2.7.4, versionCode 153 (2026-11-16). No current build, lint, JVM or device results are asserted here. The overseer will record the source SHA, environment, actual test counts, lint results, report paths and device limits after validation.

```sh
./gradlew --no-daemon :app:testDebugUnitTest :core:test :app:lint :app:assembleDebug
./gradlew --no-daemon :app:lintRelease
python3 tools/audit_structure.py
python3 -m unittest discover -s tools -p 'test_*.py'
```

For the final integrated gate, add `clean` before the module tasks. These are keyless validation commands; optimized release signing and artifact verification happen only in GitHub Actions. Measurements and installation must use the exact downloaded Actions APK bytes.

## Historical 2.0.0 validation — 2026-11-04

Optimized stable artifact, versionCode **102**, installed on TCL C755 / Android 14. APK **1,793,626 bytes (1.71 MiB)**. Installed SHA256 matches `947d920ff1e4a3a6b81a246851c88005daaf7a92be223aa185bc50806dc17d70`.

Final `scripts/build.ps1 -Checks`: 103s, **148 JVM tests**, zero failures/errors, lint zero errors and one existing TV-orientation warning. Structure audit, two Python tool tests and whitespace checks pass. [Scope and primary sources](archive/RELEASE-2.0.md).

## UI and state

Screenshots verify translucent Home/dock, artwork-coloured outward glow, wrapped settings details and the corrected **3dp translucent progress track without an opaque backing or outline**. Progress retains watched-percentage state and standard progress semantics. No extra focus target or image decoding is introduced.

Scoped resumed-activity dumps verify favorites cancel, Appearance/Back, Home-control/Back, long recovery detail and About/Back. XML on TCL can report underlying Home instead of dialogs; these are not claimed as passing dialog XML assertions. Home-control service identity remains unchanged. Accounts, favorites, channel and wallpaper preferences were not edited. No integration enabled or app uninstalled.

The final progress correction changes paint only; the settings checks were performed on code 101 with identical settings code. Geometry and navigation checks also run on the final code 102 artifact. The final artifact rendered zero frames during a two-second Android Settings foreground sample and returned to Home successfully. Six icon frames are centered within 0.5 physical pixels; settled rows contain whole visible slots. Physical remote repeats, all display/font sizes, a full TalkBack session and the older-Android glow fallback were not verified.

## Same warm navigation scenario

24 paced Right/Left moves in the first channel, optimized artifacts on the same TV:

| Version | Frames / janky | p95 / p99 |
| --- | --- | --- |
| 1.3 baseline, sample 1 | 384 / 24 (6.25%) | 40 / not retained here |
| 1.3 baseline, sample 2 | 379 / 25 (6.60%) | 38 / not retained here |
| Initial outward glow beta 5 | 951 / 49 (5.15%) | 53 / 61ms |
| Single-pass glow beta 6 | 960 / 12 (1.25%) | 46 / 46ms |
| 2.0 final (102) | 983 / 19 (1.93%) | 46 / 48ms |

**Home smoothness is not claimed fixed.** Continuous focus glow increases rendered-frame count, so jank percentages cannot establish improvement against 1.3. Final p95 is worse than those original samples. The single-pass glow costs less than the first outward-glow implementation, but still adds work. Automatic wallpaper changes and single samples limit causal comparisons; vendor GPU counters are invalid. Composition/layout and buffer waits remain investigation targets.

Ignored evidence: `artifacts/profiling/2.0-final-*`, `artifacts/2.0-*.png`, scoped settings dumps, final navigation checks and install receipt. [Preview measurements and background-motion check](archive/VALIDATION-1.4-BETA6.md) retain earlier evidence. Home key filtering retains its held-volume repeat tradeoff.
