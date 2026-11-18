# AI Handover & Architectural Continuity

For repository navigation, see the [documentation index](docs/README.md) and [tool index](tools/README.md).

## Project State Overview
- **Repository**: `matrix-launcher` (Independent Kotlin / Jetpack Compose for Android TV).
- **Current Version**: `v2.7.6` (`versionCode 155`, `releaseChannel=stable`).
- **Target Device**: TCL Android TV (`192.168.29.219:5555`).
- **Application Package**: `com.matrix.launcher`
- **Main Activity**: `com.matrix.launcher.ComposeHomeActivity` (alias: `com.gathin.tvhome.ComposeHomeActivity`).

---

## Critical Invariants & Architecture Rules

### 1. D-pad Card Navigation & Focus Memory
- **Row-to-Row Retention**: Favorites row and channel rows must never reset to index 0 when navigating vertically.
  - Favorites row tracks `focusedFavId` (`rememberSaveable`) and `favRetained` (`FocusRequester`), passed as `fallback` to `MatrixFocusRow`.
  - Channel rows pass remembered card `retained` focus requester as `fallback`.
  - In `MatrixFocusRow.kt`: `focusProperties { enter = { if (fallback != FocusRequester.Default) fallback else FocusRequester.Default } }` ensures entering a row lands on the remembered card.
  - Rows use `LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f)` so nearby and retained items stay composed.
- **Boundary Traps**: Header Up (`FocusDirection.Up`) and bottom channel Down (`FocusDirection.Down`) cancel focus change to prevent escaping into the void or traps.

### 2. Remote Long-Press Key Event Isolation
- **Problem**: Opening a dialog on TV while holding D-pad Center causes trailing `ACTION_UP` or repeated `ACTION_DOWN` events to hit the newly opened dialog's first focused option.
- **Solution**:
  - `MatrixHomeScreen.kt` dialog root contains an `onPreviewKeyEvent` filter with `sawFreshDown` tracking. Any `ACTION_UP` or repeated `ACTION_DOWN` (repeatCount > 0) arriving before a fresh press occurred inside the dialog is consumed and dropped.
  - `MatrixMenu.kt` also implements `sawMenuDown` guard on its `LazyColumn`.

### 3. AOT Optimization Manager (Stremio & Nuvio)
- **Local ADB Loopback**: `AotOptimizationManager.kt` uses embedded `dadb` client connecting to `127.0.0.1:5555` with self-managed RSA keys. No Shizuku, Termux, or PC required.
- **Automatic Recompilation**: `AotPackageUpdateReceiver` in `AndroidManifest.xml` listens for `ACTION_PACKAGE_REPLACED`. Whenever `com.stremio.one` or `com.nuvio.tv` is updated, it automatically executes:
  ```bash
  cmd package compile -m speed -f <package>
  ```
- **In-Launcher UI**: Accessed via TV **Settings → AOT Performance** (`aot-control` page in `MatrixHomeScreen.kt`).

### 4. Build & Toolchain
- **Gradle 9.0 Clean**: All Groovy build script assignments in `app/build.gradle` use `=` assignment syntax (e.g. `namespace = 'com.matrix.launcher'`, `compose = true`, `minifyEnabled = true`).
- **Dependencies**: Java 17 toolchain, Android SDK 36, compileSdk 36, targetSdk 34, minSdk 26.
- **Signing**: Existing release keys/passwords are GitHub Actions secrets only. Local Preview, Check and Test modes remain keyless; `build.ps1 -Checks` selects Release mode and is blocked locally; the Actions release workflow builds and verifies the optimized APK directly. Use the exact verified optimized Actions APK for device checks. The next release requires a clean install and settings loss over retired signer `15198ed1…`; the new signer begins `bd4e6bf1…`. See [installation guidance](docs/NEW-TV-INSTALL.md).
- **Testing on TV**: Strictly adhere to user requirement: **Do NOT take screenshots for testing; use UI Automator inspection dumps and dumpsys** (`dumpsys activity com.matrix.launcher --matrix-ui`).

---

## Quick Reference Commands

```powershell
# Keyless validation; signed releases are GitHub Actions only
./gradlew.bat --no-daemon :app:testDebugUnitTest :core:test :app:lint :app:assembleDebug

# Connect to TCL TV
adb connect 192.168.29.219:5555

# Inspect active launcher UI state & focus
adb -s 192.168.29.219:5555 shell dumpsys activity com.matrix.launcher --matrix-ui

# Check AOT status of Stremio & Nuvio
adb -s 192.168.29.219:5555 shell dumpsys package com.stremio.one | grep -E "status=|reason="
adb -s 192.168.29.219:5555 shell dumpsys package com.nuvio.tv | grep -E "status=|reason="
```
