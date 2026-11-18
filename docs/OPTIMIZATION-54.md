> Release status: these tested fixes are included in stable 1.1.0. Preview labels and measurements below describe the original validation runs.

# Remaining-fixes preview — beta 55 (beta 54 experiment rejected)

Published beta 53 remains unchanged. This local preview addresses the startup overlay, SIMKL cache-retention and focus-layout work recorded in its audit. No additional feature menu, dependency, permission, Home mode or media launch is introduced.

## Startup configuration

RemoteConfig.Loader owns cancellable generation tokens per Activity/service. A shared single worker reads/parses the bounded config overlay; obsolete queued requests are skipped and stale results are rejected after replacement, Activity destruction or service unbinding. Only application context is used for I/O. Configuration is applied on the main thread, including accessibility/gamepad/receiver changes. The worker exits after 15 seconds idle. Direct-boot loading still waits for user unlock. The Compose Home-control page already read on Dispatchers.IO and keeps that path.

This removes synchronous config.json reads from Activity/service startup callbacks; it does not claim to remove every SharedPreferences or PackageManager/Settings Binder operation from startup.

## SIMKL committed snapshot budget

SimklDiskCache retains at most 96 MiB and 128 committed JSON snapshots, with an 8 MiB entry limit. Interrupted part files and already-unreadable oversized legacy snapshots are cleaned on first cache use, off main. Oldest unpinned pages are evicted first. Retained keys are activities, the list index, the three library snapshots and the first page of up to four selected Home lists. Tokens/preferences are outside this cache. These nine protected entries fit below the byte/count caps at the per-entry limit.

A replacement reserves space before writing and uses a unique temporary file plus atomic replacement where supported; an oversized response is served live without replacing the previous good cached snapshot. Failed deletions/protected overflow prevent new cache growth rather than deleting retained data. A temporary transfer can add up to one entry above the committed-byte budget. Filesystem tests cover age/count eviction, retained-data protection, replacement, oversize rejection and interrupted-transfer cleanup. JVM filesystem tests do not certify every Android filesystem failure mode.

## Focus/layout experiment

A new beta 53 baseline trace identifies a 70.716 ms main-thread focus event, 67.419 ms CPU, with 66.186 ms nested measure/layout. Six painter setups and seven text measurements occur in that subtree. Main-thread urgent/nested prefetch is a stronger current target than adding/changing the halo. Trace has 35 unavailable-event setup notices; ART/thermal coverage is incomplete.

Resolved Foundation is 1.10.5. Beta 54 tested visible-column nested prefetch. The matched 160-dispatch trace regressed: average focus 12.254→12.567 ms, maximum 70.716→90.070 ms, urgent-prefetch total 536.9→768.2 ms. That strategy and its import were removed, not retained as another tuning option.

Beta 55 keeps the default nested strategy and ahead .5, while testing behind .5→1.0 to retain roughly one more visited row during reversals. Actual slot geometry remains unchanged. Row IDs and portrait classification are cached by card data; a 128-entry URI→aspect float cache seeds fitted artwork on repeat visits without retaining extra bitmaps. Measurements below determine whether this replacement stays. Newer Foundation 1.13 documentation deprecates the old strategy API, but no dependency upgrade is part of this fix.

Primary references: [nested prefetch API](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary), [Compose phase deferral](https://developer.android.com/develop/ui/compose/performance/phases), [lazy cache-window guidance](https://developer.android.com/blog/posts/deeper-performance-considerations).

## Validation

Beta 55 (versionCode 83) passed all 130 JVM tests and lint (zero errors, existing orientation warning). Optimized APK 1,776,382 bytes, SHA256 `717e2f99441830b0501cb2e54881a95a1a7775fbab89f43c9edcd849d3132b95`; installed bytes match. Seven-row traversal, independent row memory (row one/card two and row two/card one), two-stage Back, settled whole slots, centered icons and 40 read-only route checkpoints pass.

The initially worse beta 55 trace is preserved. A second identical 160-dispatch run after wider runtime warmup measured average focus wall 8.831 ms vs beta 53's 12.254 ms, CPU 8.605 vs 12.042 ms, maximum wall 61.487 vs 70.716 ms, p95 46.912 vs 47.877 ms. Painter setup count 263 vs 618; deactivations 212 vs 357. This supports the combined reuse change in warm navigation, but retention/aspect-cache/warmup effects are not isolated. Urgent-prefetch maximum remains 69.455 ms vs 46.914 ms; a 120.7 ms frame-timeline outlier remains. It is not a smooth-60fps certification or a claim that all jank is gone.

Post-warm PSS was 104,399 KiB (~102 MiB), with one Activity. Beta 53's 123,820 KiB snapshot had two Activities, so no causal memory-reduction claim is made. Warmup55 graphics stress sample was 46/168 janky frames (27.38%, p95 200 ms); these different workload/cache conditions are retained rather than replaced with the favorable focus-time comparison.

The app already includes ProfileInstaller and packaged baseline profiles. The TV initially reported `verify/install`. After finishing trace captures, the documented app-only SAVE_PROFILE broadcast returned 12, then `cmd package compile -f -m speed-profile` returned Success and actual dex status changed to `speed-profile/cmdline`. No global dexopt property or animation scale was changed; no frame gain is attributed to compilation without a post-reboot comparison. See [ProfileInstallReceiver](https://developer.android.com/reference/androidx/profileinstaller/ProfileInstallReceiver) and [ART service](https://source.android.com/docs/core/runtime/configure/art-service).

The approved reboot completed: Matrix was already foreground before any injected Home, Home recovery/service binding and all seven rows returned, no crashed-service entry was present, and speed-profile/cmdline persisted. Post-reboot seven-row stress and geometry checks pass. That stress sample reported 31/176 janky frames (17.61%, p95 85 ms); it is a separate compilation/reboot condition, not an isolated attribution to source changes. One final Home/media frame was inspected for fitted artwork, corners, rim gap and progress placement. After all automatic input stopped, the user confirmed Home, Inputs and Settings work and navigation feels smoother. Private traces, SQL, captures, receipt and device diagnostics remain under artifacts/audit54. Beta 54/55 were not separately published; the retained beta 55 fixes are included in stable 1.1.0.


## Reapplying app-only compilation after a later update

The APK already contains baseline profiles; no additional profile library or always-running optimizer was added. After using the launcher enough to warm its normal paths, these commands save its current profile and request profile-guided compilation:

```powershell
adb -s <TV-IP>:5555 shell am broadcast -a androidx.profileinstaller.action.SAVE_PROFILE -n com.matrix.launcher/androidx.profileinstaller.ProfileInstallReceiver
adb -s <TV-IP>:5555 shell cmd package compile -f -m speed-profile com.matrix.launcher
adb -s <TV-IP>:5555 shell dumpsys package com.matrix.launcher
```

Allow a short interval after SAVE_PROFILE. Inspect the actual ABI dexopt status; Success alone is not proof of compilation. This TV now reports speed-profile, but other firmware may retain verify. Updated compilation was verified after the user-approved reboot; do not silently force-stop the Home helper or reboot a playing TV. The command is per-app, does not alter global animation scales/dexopt properties, and does not change APK bytes. New app versions can reset compiled state.
