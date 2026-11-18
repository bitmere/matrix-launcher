# Testing

Run `./build.ps1 -Mode Test` for core and Android backend JVM tests. Focus core work with `./build.ps1 -Mode Test -Tests com.matrix.launcher.core.ReorderSessionTest`. Keyless current validation uses the repository wrapper:

```sh
./gradlew --no-daemon :app:testDebugUnitTest :core:test :app:lint :app:assembleDebug
./gradlew --no-daemon :app:lintRelease
python3 tools/audit_structure.py
python3 -m unittest discover -s tools -p 'test_*.py'
```

Add `clean` before the module tasks for the final integrated gate. Use JDK 17 and Android SDK platform 36. Preview, Check and Test modes need no release credentials (`./build.ps1 -Mode Check` runs JVM tests and lint without packaging). Signing keys and passwords exist only in GitHub Actions secrets; local builds cannot produce the signed release. `build.ps1 -Checks` selects Release mode and is blocked locally; use `-Mode Check` for keyless checks. The Actions release workflow invokes the optimized build and verification directly. Download and verify that run's APK for device measurements; never substitute a preview or locally rebuilt APK. These policy tests do not prove Compose focus/rendering or TV firmware behavior.

Use an isolated emulator for navigation, settings/form states and visual checks. The existing Matrix_Compose_Review AVD is TV-shaped but uses a phone system image; it cannot validate vendor startup, actual TV providers or physical remote repeats. Test both a fresh app state and saved preferences without copying account secrets into fixtures.

`tools/compose_smoke.py` requires Matrix in the foreground and uses an explicit serial, XML metadata first and D-pad keys. It checks foreground before every input/launch command. Screenshots are for actual visual rendering, not logical text presence. On TCL, an underlying Home hierarchy is not proof a dialog is correct; use scoped activity/window checks and one screenshot when necessary.

Regression checklist: all main settings, Back history/focus, favorites cancel/save/reorder, independent channel-row memory, Back-to-row-start then Home, pagination, Home/Inputs remote routing, empty feeds, no SIMKL account, QR expiry/cancel, quota cooldown/cache fallback, wallpaper source/rotation, app install/remove, background/foreground and process restart. Test clock/screensaver separately. Physical Home/volume tests are required only when changing/activating the helper, and must not be inferred from injected keys.

Device evidence, screenshots, installed APK receipts, tokens and dumps belong only in ignored artifacts. Never publish test accounts or device dumps. A developer preview APK is for iteration, not publication.

Current measured results await the overseer validation run: [validation commands and results placeholder](docs/VALIDATION.md). The same page preserves the **historical 2.0.0 report (2026-11-04)**; its measurements are not current 2.7.4 results.

For a playback-free warm Home-row sample, use `python tools/measure_navigation.py --adb <adb-path> --serial <TV-IP>:5555 --output artifacts/profiling/<label>`. It requires Matrix already in the foreground, wakes the TV, opens this preview, checks the focused package, saves a warm-state capture and records gfxinfo after 24 paced D-pad moves. Optionally pass the official Perfetto `record_android_trace` helper with `--trace-helper`; traces stay under ignored artifacts. Confirm the warm capture before comparing runs. A single device sample is not a universal FPS claim, and unreliable vendor GPU counters must not be presented as measurements.

Home boundary regression: reach both settled endpoints of Favorites (including Edit Favorites), each channel and the header; extra Left/Right must remain in that row. Verify Up/Down and Back still work. Check both manual reload entry points for start/result feedback, with no stale loading notification after completion.
