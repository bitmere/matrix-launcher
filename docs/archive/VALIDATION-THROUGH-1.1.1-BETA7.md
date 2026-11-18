# Historical validation through beta 7

Recorded statuses below describe each validation run; beta 7 was subsequently published. Stable 1.2.0 validation is in [the current record](../VALIDATION.md).

# Local 1.1.1-beta.7 — remote-friendly wallpaper browser

VersionCode **92**. Optimized APK **1,793,698 bytes**, installed SHA256 `10ff0e6bcc532fd4cd44c4085eecf28a04f8c45711c2d85a91a7c510ec71712f`. Final Checks build: 81.3 seconds; **144 JVM tests** pass, zero lint errors and the existing orientation warning. Structure audit, two tool tests and whitespace checks pass.

Replaces beta 6's opened-but-not-remotely-usable system picker. User granted image access through TCL's native permission dialog (its generic label says photos and videos; Matrix requests images only). Matrix listed Pictures and Downloader albums. Dump-based D-pad checks moved grid focus through indices 0, 1, 4, 3, 0, opened Downloader and returned to albums with Back. Refresh exposed a temporary fixture; a selective screenshot confirmed the blue test image was focused. Remote OK copied it into the existing JPEG pipeline, verified 2x2, then returned to albums. Original local collection restored after deleting the recorded fixture/import/MediaStore row. Diagnostic images from this check removed from the TV too.

All input stopped on Matrix Home; Home service/recovery, seven rows and Automatic media choice retained. No personal image imported, selected background change, playback, account/default-Home change or reboot. Photo access remains as granted by the user; no video/write/all-files permission. Public release unchanged; local/unpublished.

## Earlier local previews

- **Beta 6 / code 91:** APK 1,793,578 bytes, SHA256 `253c656c08757c679443dc6f5895235d2b479a1555265c6fc455893cb16227ae`; 144 tests, zero lint errors and existing orientation warning, build 74.4s. Native picker opened/listed 24 visible images, but actual remote usability was not certified and is corrected above. Saved gallery/current-image reapply and shared-image confirmation/import/cancel/remote-URI rejection passed. Temporary 2x2 fixture/import removed; original collection/background retained. [Wallpaper evidence and limits](../WALLPAPERS.md).
- **Beta 5 / code 90:** APK 1,776,514 bytes, SHA256 `52f621a5785c5297713dee7751f3fe6c9d86bc85b2917450a8cde585f287384a`; 141 tests, zero lint errors and existing orientation warning, build 105.4s. Canonical Matrix implementation namespace installed with four stable service bridges and one Home alias. Secure settings, selected services/default Home/screensaver matched pre-update snapshots. Legacy/new routes, seven rows, SIMKL, Automatic media, capture/recovery passed; user confirmed repeated real Home, Inputs and Settings. [Namespace compatibility](../NAMESPACE.md).
- **Beta 4:** Reload Home and settings commands use the shared MatrixButton/SettingsPage typography and geometry; before/after visual review retained in ignored artifacts. [UI review](../UI-REVIEW.md).
- **Betas 1–3:** Configurable Automatic/Nuvio/Stremio media target, native publisher routes, catalog resolution and warm-Activity handling tested. Stremio title links use NEW_TASK/CLEAR_TOP without SINGLE_TOP; Nuvio fresh details use content extras without playback IDs. Search movie/series and SIMKL-row links tested without Play, and Automatic/Nuvio preference restored. [Media app contract and validation](../MEDIA-APPS.md).

All local previews remain unpublished. Stable 1.1.0 and its release evidence below are preserved.

---

# Stable 1.1.0 validation — 2026-11-04

VersionCode **85**; package the then-current package. Optimized release APK **1,776,362 bytes** (about 1.69 MiB), minified and resource-shrunk with the existing signing identity.

SHA256: `edb05da2216e78c42abdd6af9d827b650ff09f37e730149d02e3fbd83f8ca020`.

## Final release checks

- Final `build.ps1 -Checks` succeeded in 76.3 seconds. All **133 JVM tests** pass; unchanged tasks reused Gradle's successful outputs. Two dump-parser tests and the component/shared-control audit pass.
- Lint: **zero errors**, one existing TV-orientation warning. Relative documentation links and whitespace checks pass.
- Exact stable APK installed and its on-device SHA256 verified. Package manager reports 1.1.0/code85. Accounts/preferences, Home capture and recovery retained.
- App-scoped text dumps confirm resumed Home, seven rows, connected Home capture and recovery. Brief Down/Up navigation passes; automatic input stopped. No playback, microphone, account/profile or Home-mode action in this final release check. No reboot repeated: production code is unchanged from the validated beta 56; release version/metadata changed.
- The immediately preceding beta 56 verified actual warm Search/SIMKL title switching into Nuvio. Beta 55 covered a reboot and real Home/Inputs/Settings; the user confirmed smoother navigation. These are historical run conditions, not new stable-version reboot or physical-key tests.

## Known limits and evidence

[Release notes](../RELEASE-1.1.md) list current limitations. [Nuvio handoff](../NUVIO-ROUTING.md) records the version-tested compatibility route, rejected task reset and corrected harness failures. [Optimization measurements](../OPTIMIZATION-54.md) retain warmup variance and residual frame spikes. No guarantee of zero jank, isolated RAM savings or every firmware/provider combination.

Detailed beta 34–56 evidence is retained in [recent validation history](../archive/VALIDATION-BETA-34-56.md); [earlier validation](../archive/VALIDATION-HISTORY.md) is preserved separately. Device dumps, traces, screenshots and installation receipts remain only in ignored artifacts.
