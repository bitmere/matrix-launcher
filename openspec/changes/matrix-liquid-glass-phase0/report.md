# Phase 0 handoff — implementation ready, adoption unproven

## Exact Haze surface

Only `dev.chrisbanes.haze:haze:2.1.0` and
`dev.chrisbanes.haze:haze-blur:2.1.0` are added. No optical Glass API is used,
so `haze-glass` is intentionally omitted.

- `dev.chrisbanes.haze.rememberHazeState()` (no arguments).
- `Modifier.hazeSource(state = hazeState)` with default zIndex/key and no
  experimental input sharing.
- `HazeInput.Sources(hazeState)` with default selection/retention, explicitly
  source-backed; no native window backdrop backend.
- `dev.chrisbanes.haze.blur.HazeBlurStyle { blurRadius(18.dp);
  noiseFactor(0f); colorEffects(emptyList()) }`, remembered once.
- `dev.chrisbanes.haze.blur.hazeBlur(input = ..., style = ...)` with default
  performance mode and layer expansion. Default rectangular edge treatment
  bounds output to the header while capture expansion supplies blur edges.

The brief's old `hazeEffect(state)` spelling does not match 2.1.0. Its
[versioned API](https://chrisbanes.github.io/haze/2.1.0/api/haze/dev.chrisbanes.haze/haze-effect.html)
is generic and requires a renderer factory. The public built-in
[`hazeBlur`](https://chrisbanes.github.io/haze/2.1.0/api/haze-blur/dev.chrisbanes.haze.blur/haze-blur.html)
wraps that core effect internally. The
[tagged implementation](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/haze-blur/src/commonMain/kotlin/dev/chrisbanes/haze/blur/HazeBlur.kt)
confirms this; no custom renderer or opt-in is needed by our calls.

Other API evidence:
[rememberHazeState](https://chrisbanes.github.io/haze/2.1.0/api/haze/dev.chrisbanes.haze/remember-haze-state.html),
[hazeSource](https://chrisbanes.github.io/haze/2.1.0/api/haze/dev.chrisbanes.haze/haze-source.html),
[tagged input declarations](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/haze/src/commonMain/kotlin/dev/chrisbanes/haze/HazeInput.kt),
[tagged style declarations](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/haze-blur/src/commonMain/kotlin/dev/chrisbanes/haze/blur/HazeBlurStyle.kt),
[tagged defaults](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/haze-blur/src/commonMain/kotlin/dev/chrisbanes/haze/blur/HazeBlurDefaults.kt).

## Compatibility gate

The [2.1.0 changelog](https://chrisbanes.github.io/haze/2.1.0/changelog/)
and [tagged version catalog](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/gradle/libs.versions.toml)
confirm Kotlin 2.4.21 / Compose Multiplatform 1.12.2; the latter also records
Jetpack Compose 1.12.1. Matrix stays on Kotlin 2.2.20, BOM 2026.03.00,
tv-material 1.1.0, minSdk 26, target 34 and compileSdk 36.

The [blur module build](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/haze-blur/build.gradle.kts)
exposes Compose UI as an API dependency. This is material version skew,
not evidence of compatibility. The Maven Android POM could not be retrieved
through the web tool. Published Kotlin metadata, AAR minimum requirements,
resolved AndroidX versions and binary linkage remain unverified. A successful
build that silently raises Compose versions would not prove the requested
pinned-stack compatibility. Coordinator must inspect dependency resolution as
well as compile and run tests. No stack upgrades, exclusions, forced versions,
or metadata-check bypasses were added. Do not adopt until these gates pass.

## Behavior and fallback

The header is composed before the background sibling for focus traversal and
drawn above it with zIndex. The source encloses MatrixBackdropSurface's own
gradient plus photo/dim/list; the header is outside that source. Its existing
icon gradients and control code remain intact. There was no full-width header
gradient in the baseline, so none was invented. Haze renders behind sharp
header content, including those existing gradients.

List padding uses measured header height plus the original 34dp margin and
100dp spacing. Measurement updates after layout; first-frame settling and font
scale changes require device checks. Reveal calculations subtract the header
region from both offset and viewport. The original header/favorites top-return,
initialFocus attachment and Back logic remain. Common-parent key preview keeps
repeat deferral/inspection active for the now-separated header as well as rows.

Eligibility is pure `sdkInt >= 31 && !safeMode && !lowRam`; animations and
wallpaper keep their old decisions. Home also checks the actual SDK defensively.
On API <=30 or CHEAP, neither source nor effect is attached: the static icon
gradients remain, with zero live capture. The full STATIC tier is deferred.

Haze itself defaults to blur disabled and a translucent fallback scrim on
API <=30; RenderScript there requires explicit enablement, which this spike
never does. See [2.1.0 platform documentation](https://chrisbanes.github.io/haze/2.1.0/blur/platforms/).
Our older-device fallback bypasses Haze rather than depending on its scrim.
The same documentation notes extra API 31 recapture work and a software-canvas
RenderScript backend on newer APIs; hardware-TV profiling remains necessary.

## Files

- `app/build.gradle`: two pinned Haze dependencies only.
- `MatrixCapabilities.kt`: SDK-aware liveBlur eligibility, existing effects unchanged.
- `ComposeHomeActivity.kt`: pass actual SDK to the decision.
- `MatrixHomeSurface.kt`: floating header, single source/effect, measured spacing,
  overlay-aware reveal and shared key preview.
- `MatrixCapabilitiesTest.kt`: existing tests pass SDK explicitly; eligibility
  matrix covers API 26/30/31/32/34/36 and all safe-mode/low-RAM combinations.
- `THIRD_PARTY_NOTICES.md`: Haze attribution and Apache-2.0 license/source links.
- This OpenSpec directory: scoped proposal, tasks, glass-material spec and report.

## Review and verification

`tools/audit_structure.py` passed (17 manifest components, 112 app classes,
core Android-free). `git diff --check` passed. Impeccable detector scanned the
Kotlin target without findings, but only regex rules ran; no parsed-page or
rendered contrast claim is made. No screenshot goldens were found by the local
filename scan. Project-local skill link was unavailable; installed user-level
Impeccable supplied context, craft floor and native audit guidance.

Source review passes: critique/layout checked one bounded overlay and preserved
spacing; typeset/clarify preserved typography and labels; simplify kept only two
modules and one boolean; polish retained icon/control geometry; harden checked
empty favorites, Now Playing disappearance, Back and SDK guards; audit checked
capture ownership, no new bitmap work and unchanged core boundaries. Runtime
focus/contrast and optical centering remain open, not passed by source review.

No Gradle, compilation, JVM execution, lint, install, TV measurement or visual
capture was run. Frame timings, GPU/native memory, API 31 scrolling, old TCL
fallback, D-pad XML traces and screenshot pixel bounds are coordinator gates.
No commit/push/PR/merge/tag/release, version or preference changes were made.

Deviations: the documented 2.1.0 built-in `hazeBlur` replaces the brief's old
`hazeEffect` call; unused `haze-glass` is omitted as permitted. No new header
base gradient was added because the existing treatment belongs to its buttons.
The spike is implemented for review, not proven compatible or approved for adoption.

## Spike verdict — FAILED (coordinator gate, 2026-11-28)

`:app:testDebugUnitTest --tests "com.matrix.launcher.MatrixCapabilitiesTest"`
fails at configuration/dependency resolution; `BUILD FAILED`, exit 1.
Full log: `spike-build-failure.log` in this directory. 21 errors, all of one
kind: Haze 2.1.0 and the versions it drags in require compileSdk 37 and
Android Gradle Plugin 9.1.0+, while Matrix pins compileSdk 36 and AGP 8.13.2.

- Haze's own artifacts require compileSdk 37: `haze-android:2.1.0`,
  `haze-blur-android:2.1.0`, `haze-utils-android:2.1.0`.
- Haze's transitive upgrades require AGP 9.1.0+ and compileSdk 37:
  Compose `ui/animation/runtime` 1.10.5 -> **1.12.1** (dependency tree shows
  every Compose module upgraded from the BOM 2026.03.00 pin), `core` /
  `core-ktx` **1.19.1**, `lifecycle-runtime-compose-android` **2.11.0**,
  plus Compose runtime **1.12.2** via Compose Multiplatform.
- No code in the spike was reached: compilation never ran, so Haze API
  compatibility, frame budget, and the API ≤30 fallback are all still
  unverified.

Per the task brief the spike stops here: Phase 1 did NOT start, nothing was
committed or pushed, no fallback was silently applied. The branch
`matrix-liquid-glass-phase1` stays un-pushed/unfinished at this state.
Re-scope options for the user (not decided here): (a) upgrade the toolchain
to AGP 9.1.0+ / compileSdk 37 (a toolchain migration, outside the Phase 1
scope as approved); (b) try an older Haze release built against the pinned
Compose/BOM (spec pins 2.1.0 — a spec change); (c) drop Haze and build the
honest wallpaper-frost system (STATIC everywhere) as the live-blur fallback.

## Retry verdict (2026-11-28, after toolchain migration) — SPIKE PASSED

Toolchain migrated in-branch (AGP 9.1.1, Gradle 9.3.1, compileSdk 37, Kotlin 2.4.21; `migration.md` in this directory). Migration gates on the migrated tree: 336 tests 0 failures, lint 0 errors / 1 warning, `assembleDebug` builds, `audit_structure.py` passed, tools tests 56 OK. One migration-caused lint break fixed properly: two `NonObservableLocale` errors (`MatrixClockScreen.kt:29`, `MatrixHomeSurface.kt:66`) fixed with the AndroidX-recommended `LocalLocale.current.platformLocale`.

Spike gates on the migrated stack:
- (a) Compiles: PASSED. Haze 2.1.0 resolves (Compose ui/runtime 1.12.1, lifecycle-runtime-compose 2.11.0, core-ktx 1.19.1, Kotlin stdlib 2.4.21 — all confirmed from the resolved graph); full build green.
- (b) Frame budget: no TV in this VM — honestly unverified on-device. Verifiable: exactly one live effect (floating header, 18dp `hazeBlur`), one `hazeSource`, no per-frame bitmap work in app code, no animated blur radius. On-device frame timing still needs a TV.
- (c) API ≤30 fallback readable: PASSED at code level. The floating header gets a 70% black fill when `liveBlur` is false (same fill as the clock overlay, inside the spec's 0.60–0.75 range); the live path is unchanged. No JVM test can render the header; the eligibility matrix covers `liveBlur = false`.
- Known review note fixed: `rememberHazeState()` is now remembered unconditionally; only the modifiers are gated (verified against Haze 2.1.0 source: unattached state allocates only bookkeeping collections).

Spike verdict: ADOPT Haze. Phase 1 proceeds on this branch.
