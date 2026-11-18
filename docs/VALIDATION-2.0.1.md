# 2.0.1-beta.2 patch preview

Installed locally on TCL C755 / Android 14; **not published**. VersionCode 104. Optimized APK: **1,793,630 bytes (1.71 MiB)**. Installed SHA256 matches `d1be54b45f819ff673665b05c1c7f332e4e46fb79db1a8bedca6319b4b80e39e`.

## Fixes

- Manual toolbar/Settings reload announces start and completion. Settings retains the last manual result, including provider status; interrupted/failed requests are not reported as successful. Duplicate activation during loading is ignored. New notifications replace stale ones.
- Shared Home rows block Left/Right exits at their endpoints; Up/Down remains available. The header uses the same boundary rule. Edit Favorites updates Home focus position/title, so Back resets the dock correctly.
- Grid, form and reorder navigation is not globally trapped: those surfaces have different valid directional actions, including the reorder Save shortcut.

## Evidence and limits

The reported defect was reproduced on stable 2.0.0: repeated Right from Favorites reached the header. Beta 1 scoped dumps verify both endpoints of Favorites, the header and all seven current channel rows, with vertical movement and Back intact. Final beta 2 changes only notification replacement relative to that tested navigation code; dock/header checks and both Settings and toolbar reload are repeated on the final artifact. Screenshots confirm the completion notification replaces the loading notification, and Settings retains the result.

Full optimized checks: **148 tests**, zero failures/errors, lint zero errors and one existing warning. Structure/whitespace checks pass. Device dumps and screenshots stay in ignored `artifacts/patch-*`. TCL's toolbar XML capture returned a null root; scoped dumps and screenshots supplement it, not a claimed passing XML assertion. No accounts/preferences were cleared or integrations enabled.

Home jank remains unresolved; this patch makes no performance claim. [Stable 2.0 measurements and remaining coverage](VALIDATION.md) remain current. Full TalkBack, older-Android fallback, all grid/form configurations and physical held-key behavior are not comprehensively device-tested.

Reference: [Compose focus-group boundaries](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-group).
