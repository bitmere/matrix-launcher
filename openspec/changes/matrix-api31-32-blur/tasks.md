# Tasks — matrix-api31-32-blur

## Implementation
- [x] Read the investigation report (`api31-32-blur-investigation.md` §5) before touching code.
- [x] `MatrixGpuQualification.kt`: init gate `>= 33` → `>= 31`, comment rewritten with the fence-less basis and the API 31 provisional note.
- [x] `MatrixGpuQualification.kt`: `onStateChanged` guard `< 33` → `< 31`.
- [x] `MatrixGpuQualification.kt`: `@RequiresApi(33)` → `@RequiresApi(31)` on `tryProbe()` / `measureBlur()`.
- [x] `MatrixGpuQualification.kt`: `measureBlur()` branches — fence path on 33+, planes touch (`output.planes[0].buffer`, capacity checked) on 31/32.
- [x] `MatrixGpuQualification.kt`: 31/32 probe runs on a dedicated daemon single thread (`MatrixGpuProbe31`, closed in `finally`); 33+ keeps `Dispatchers.Default`. Outer `withTimeout(4_000)` and all error handling unchanged.
- [x] `MatrixGpuQualification.kt`: broadcast receiver registration branches — 3-arg `RECEIVER_NOT_EXPORTED` overload on 33+ only, 2-arg overload on 31/32 (the 3-arg overload is API 33+; calling it below 33 would throw uncaught `NoSuchMethodError`). Caught by lint (`InlinedApi`) during the gates and fixed properly, not suppressed.
- [x] `MatrixCapabilitiesTest.kt`: new test `measuredGpuGetsFullOnApi31And32` — `decide(31, measuredGpu=true)` and `decide(32, measuredGpu=true)` both return FULL.
- [x] No changes to `MatrixCapabilities.kt`, `ComposeHomeActivity.kt`, or `MatrixHomeSurface.kt` — they already handle API 31 correctly.
- [x] No new permissions, no new dependencies.

## Gates (coordinator)
- [x] Full test suite: 361 tests (330 app + 30 core + 1 new), 0 failures, 0 skipped.
- [x] `:app:lintDebug` 0 errors (1 pre-existing warning: Gradle 9.9.3 available; 5 pre-existing hints).
- [x] `assembleDebug` success (APK 12,158,091 B).
- [x] `tools/audit_structure.py` passed (120 app classes, 17 manifest components, core Android-free).
- [x] `git diff --check` clean; no DebForge mention (grep-verified).
- [x] Impeccable detector on the touched main file: 0 findings (non-UI change; regex-only scan).
- [x] Lint caught a real bug during the gates (see below) — fixed properly, gates re-run green.
- [ ] Branch published; verified via the compare API (ahead 1, behind 0, exact file list).

## Gate catch — fixed properly, not suppressed

Lint (`InlinedApi`) flagged `Context.RECEIVER_NOT_EXPORTED` in `MatrixGpuQualification.kt`: the 3-arg `registerReceiver` overload is API 33+. Before this change the call was unreachable below 33 (init gate); lowering the gate to 31 exposed it, and on API 31/32 it would have thrown `NoSuchMethodError` — an `Error`, not caught by the existing `RuntimeException` handler — crashing Home at qualification start. Fixed by branching the registration (3-arg with `NOT_EXPORTED` on 33+, 2-arg on 31/32; the broadcasts are system broadcasts no other app can send). Lint re-run: warning gone, 0 errors. Full suite re-run after the fix: 361 tests, 0 failures.

## Open after the branch lands (not part of this change)
- [ ] Real-device validation of the fence-less probe on Android 12 and 12L (no TV/device in this VM).
- [ ] Real-device measurement of the API 31 Haze recapture cost; if it janks, gate the init change to `>= 32` only.
- [ ] PR / merge / release — separate approvals.
