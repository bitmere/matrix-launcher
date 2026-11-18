# Stable 1.3.0 validation

VersionCode 94; optimized APK 1,793,626 bytes, SHA256 `4722597be2f1d903875ef7565d6302a6c24e9b8bf240676a1f9d658a33759fbe`. Build checks completed in 108.6s: 148 JVM tests, zero lint errors and the existing orientation warning; structure audit and two tool tests pass.

Installed APK checksum verified. Scoped dumps passed Home entry, favorites Right/Left, wallpaper album menu and Back, then three four-direction cycles in the image grid with exact indices 0/1/4/3/0 (12 transitions). Accounts, seven rows, Automatic media target and Home binding retained. Final Home screenshot inspected. Automatic input stopped on Home; no media/image selected, account/default change or reboot. Hidden loading/error focus and cancellation-during-read paths are JVM-tested; they were not forced through live network faults on the TV. This targeted pass does not certify every screen or quantify frame-time improvement. [Review scope](../RELEASE-1.3.md).

---

# Stable 1.2.0 validation — 2026-11-04

VersionCode **93**. Optimized/resource-shrunk APK **1,793,694 bytes** (about 1.71 MiB). SHA256 `f6791636d2e700bf9ed70100b4e189700a94a68a4ed13256512f517c47a8ad30`. Final Checks build succeeded in **71.0 seconds**. **144 JVM tests** pass, Android lint has zero errors and the existing TV-orientation warning. Structure audit, two tool tests, relative documentation links and whitespace checks pass.

## Installation and retained state

Installed the exact stable APK over beta 7 on TCL C755 / Android 14; the installer verified the installed base APK SHA256. Platform package metadata reports 1.2.0/code93. Before/after accessibility-service selections match exactly. Home service remains connected, recovery selected, SIMKL connected with three visible custom lists, seven Home rows and Automatic media target retained. Previously granted image access remains available. No account, Home-mode, integration or wallpaper change.

The stable build changes version metadata only; production code matches the installed beta 7. An initial scoped check stopped when Nuvio came to the foreground. After the user confirmed the remote was idle, the follow-up passed: programmatic Home entry, favorites Right/Left with exact card-index checks, Settings → Wallpapers → Import pictures, D-pad movement in the album menu, Back to Wallpapers and final Home. Scoped text dumps confirm accounts, seven rows, Automatic media target and connected Home service. Final Home screenshot was visually inspected; automatic input stopped on Home. No personal picture import, media launch, account/default change or reboot. Beta 7 provides the full temporary-image import evidence; earlier previews provide both media-target and physical Home/Inputs/Settings checks. This follow-up does not claim a new physical-remote Home test.

## Documentation and publication

Current setup/release docs now target 1.2.0. Earlier validation is preserved as [history](VALIDATION-THROUGH-1.1.1-BETA7.md); historical release pages identify their successor. Future patches use 1.2.1-beta.N; feature work uses 1.3.0-beta.N. Device diagnostics, account data and credentials stay in ignored artifacts. Release assets are the exact installed APK plus SHA256; publication additionally verifies a downloaded copy.

## Known limits

See [release notes](../RELEASE-1.2.md). This version does not claim uninterrupted 60 fps, a measured RAM reduction, TMDB live credential validation or newly tested full Continue Watching playback. TCL Home key filtering retains the known held-volume tradeoff. Direct add-on catalogs and SIMKL AI search are not implemented. No YouTube integration changes.
