# Tasks — matrix-now-playing-card

- [x] Investigate repository and Android/AOSP APIs; write findings before implementation.
- [x] Add plain-value session store, selection and command policy.
- [x] Add worker-owned Android observation, lifecycle cleanup and bounded artwork.
- [x] Preserve GuardRevival unlock/revival behavior and legacy manifest components.
- [x] Wire the pinned HomeSnapshot row and HIDDEN_ROWS / rowShown setting.
- [x] Add the scoped glass card and shared icon controls; leave global borders untouched.
- [x] Add plain JVM tests for visibility, selection, actions and stale dispatch.
- [x] Perform source review for lifecycle, focus, layout, typography, simplification,
  clarity, hardening and native accessibility; document visual gates below.
- [ ] User diff review before compiling.

## Verification — NOT RUN this round

Baseline: `matrix-now-playing-card`, main 2.7.12 at `591190c`.
All changes remain uncommitted. No Gradle, tests or lint were run, per request.

- [ ] NOT RUN: focused NowPlayingSessionTest, then full JVM suite and lint.
- [ ] NOT RUN: tools/audit_structure.py and Impeccable detector (lint-like gates
  deferred under this round's prohibition); no rendered-quality claim.
- [ ] NOT RUN: XML/D-pad on TV, arrival/removal while focused, disappearing skip,
  play/pause state changes, multiple sessions, Back and horizontal boundaries.
- [ ] NOT RUN: notification access absent/granted/revoked, unlock/reconnect and
  process recreation; low-RAM/old-Android listener availability.
- [ ] NOT RUN: YouTube, Stremio, Nuvio and live-TV session/action compatibility.
- [ ] NOT RUN: screenshot contrast and glass alignment over artwork/wallpaper,
  Android 12+ blur and old/cheap fallback, long text, large fonts and RTL.
- [ ] NOT RUN: --matrix-ui IconLayout settled offsets and glyph pixel bounds.
- [ ] NOT RUN: optimized release build, device install and exact artifact hashes.

## Source-review notes

Operate surface: one row, title-first hierarchy, source and playback state,
up to three named icon controls, no progress or idle placeholder. Shared TV
control sizing and type roles; text truncates, artwork fits without stretching.
Missing artwork retains a neutral icon; unavailable controls disappear, and the
card remains focusable without a false action. Focus stays coloured on the new
card; unchanged shared icon-button styling remains in effect.

The adapter rejects stale identities/actions and publishes no saved history.
Access loss clears state; old adapter callbacks cannot overwrite a replacement.
Registration failures clean up partially registered callbacks. Media Binder work
and thumbnail scaling run on the worker; URI images use Coil's shared loader.
Home updates from the in-memory stream without reloading TV/SIMKL feeds. Settings
visibility updates from the existing preference revision stream.

Manual source review cannot establish visual contrast, glyph centering, actual
app compliance, or successful compilation. These gates remain open for review.

## Changed files for review

Production sources under `app/src/main/java/com/matrix/launcher/`:
- `AndroidNowPlaying.kt` (new)
- `NowPlayingSession.kt` (new)
- `NowPlayingCard.kt` (new)
- `ComposeHomeActivity.kt`
- `ComposeHomeModels.kt`
- `ComposeHomeRepository.kt`
- `GuardRevival.java`
- `MatrixHomeSurface.kt`
- `MatrixPrefs.kt`
- `MatrixSettingsPages.kt`

Resources under `app/src/main/res/`:
- `drawable/ic_now_play.xml`, `drawable/ic_now_pause.xml`
- `drawable/ic_now_previous.xml`, `drawable/ic_now_next.xml`
- `drawable/ic_now_playing.xml`
- `values/now_playing_strings.xml`

Tests: `app/src/test/java/com/matrix/launcher/NowPlayingSessionTest.kt`.
OpenSpec: this change's `proposal.md`, `tasks.md`, and
`specs/home-now-playing/spec.md`.
