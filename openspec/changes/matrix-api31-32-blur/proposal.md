# Proposal — matrix-api31-32-blur

**Status:** Implemented on branch `matrix-api31-32-blur` off 2.7.17 (`f897d149`); gates pending. No PR, merge, or release authorized — branch publish only.

**User decision (2026-11-28):** "Build it for 31 and 32" — enable FULL live blur on Android 12 (API 31) and 12L (API 32), after the user challenged the Phase 1 rule that FULL needs API 33+.

## Why

Phase 1 (2.7.17) gated FULL on API 33+ because its GPU qualification used an image fence (`Image.getFence()` / `SyncFence`, both since API 33) as the only evidence of completed GPU work, and the spec treated submission time as unacceptable evidence. The investigation (`~/workspace/nuvio-way/matrix-liquid-glass-research/api31-32-blur-investigation.md`) showed that gate is much narrower than the spec's wording: exactly one thing needs API 33 — the fence lines. Everything else in the probe already works on 31/32 (`ImageReader` since 29, `HardwareRenderer` since 29, `RenderEffect` since 31, `FrameMetrics.GPU_DURATION` since 31, and `GPU_DURATION` is measured from `COMMAND_SUBMISSION_COMPLETED` to `GPU_COMPLETED`, i.e. completed-GPU evidence).

And the fence turns out not to be needed for the probe at all. The fence is only a way to wait for GPU completion with a timeout. Accessing `Image.getPlanes()` waits on the same acquire fence inside the native lock: AOSP `media/jni/android_media_ImageReader.cpp` (`ImageReader_createImagePlanes`) passes the fence fd into `lockImageFromBuffer`, and `Image_getFenceFd` — the native method behind public `getFence()` — returns exactly that same fence. So on 31/32 the probe measures submit → planes-readable wall time, which still includes GPU completion, under the same 4 ms bound. Haze 2.1.0 itself enables blur by default on 31/32, and other launchers (Lawnchair, AOSP Launcher3) blur on 31+ via `RenderEffect` with no fence qualification at all.

## What changes

All changes are inside `MatrixGpuQualification.kt` (plus one JVM test):

- Qualification gate `SDK_INT >= 33` → `>= 31` (width ≤1920, height ≤1080, memoryClass ≥256 bounds unchanged).
- Completion wait branches: API 33+ keeps the fence path; API 31/32 touches `output.planes[0].buffer` (with a capacity check so the read is explicit, not an unused expression).
- The 31/32 probe runs on a dedicated daemon single thread (`MatrixGpuProbe31`), because the planes lock has no timeout — a wedged GPU must not pin a shared pool thread. The outer `withTimeout(4_000)` still bounds the rest.
- Nothing else changes: 120-frame baseline, 24 samples / 4 ms bound, revoke-on-any-bad-frame (permanent for the process). `MatrixCapabilities.decide()` already grants FULL at `sdkInt >= 31` with `measuredGpu`; `ComposeHomeActivity` and `MatrixHomeSurface` already handle 31 correctly.

## API 31 is provisional

Haze's docs state that on API 31 only, blur reading `HazeInput.Sources` recaptures its input after *any* snapshot state change — and Home has a ticking clock plus the rotating orb, i.e. state changes every frame. That recapture cost is unmeasured ("has not been measured" — Haze's own words) and Haze advises profiling. API 32 has no such caveat. The revoke-on-bad-frame net bounds the risk (one bad frame → STATIC forever for the process), but the cost itself needs a real API 31 device test. If a real-device test shows jank on 31, the fallback is to gate the init change to `>= 32` only. This provisional status is recorded in the code comment and in the spec.

## Scope — who benefits

The user's old TCL is likely API ≤30 (Android 8–10): **this change does not alter its STATIC treatment at all** — Haze draws a scrim instead of blur on ≤30, and Matrix's STATIC tier is the right call there. The beneficiary is a newer TV on Android 12/12L. The probe itself cannot be validated by JVM tests; it needs a real Android 12 and a real Android 12L device, which this VM has no access to. That validation stays open after the branch lands.
