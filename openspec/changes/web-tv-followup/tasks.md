# Tasks — web-tv-followup

Work in order. Each task names its exact files and how to verify it. Do
not start the next task until the current one verifies. No builds run
here — verification is grep, code inspection and (where a test exists)
the test itself; the main agent compiles and runs CI after the diff
review. Draft base is main @ `84f9d57` (see proposal.md for the rebase
note); the files below are already in the draft at those versions.

1. Focus glow: stop the infinite rotation, keep the highlight.
   Files: `app/src/main/java/com/matrix/launcher/MatrixButton.kt`
   (`rememberMatrixFocusRotation`, :82-92).
   Change: the rotation animates once when focus lands — a single
   0→360 sweep over ~1.5 s (tween, LinearEasing) — and then holds at
   360f. On unfocus it snaps to 0 as today. The returned State stops
   changing after the sweep, so `drawWithContent` in `MatrixArtwork`
   (`MatrixCards.kt:355-360`) stops re-running and Home draws 0 frames
   at rest. The angle at 360 equals the angle at 0 for the glow
   shader (sin/cos periodic), so the held frame looks identical to
   the sweep start — no visual jump.
   Do NOT touch: the focus outline alpha (`animateFloatAsState` 0→1,
   holds at 1 while focused), `cardFocusScale = 1.015f`, or the
   backdrop palette change.
   Verify: read the code — no other animation keeps Home drawing at
   rest. The only other infinite animations are in
   `MatrixSearchOrb.kt` and `MatrixSearchScreen.kt` (search-only, not
   Home); the clock ticks once a minute; wallpaper rotation happens on
   Home load, not on a timer. Grep confirms `rememberMatrixFocusRotation`
   has exactly one caller (`MatrixCards.kt:319`).

2. Bench harness script.
   Files: new `tools/bench.py` (Python 3, stdlib only).
   Change: a scripted D-pad tour over adb against the TV, modelled on
   Web TV Launcher's bench approach. Steps: wake the TV, dismiss the
   keyguard if needed, force-stop the app, cold start with
   `am start -W` (record TotalTime), wait for Home, run a fixed tour
   (20 focus moves, 6 row changes, menu open/close, Home press),
   then a 30 s idle window. Collect: `dumpsys gfxinfo
   com.matrix.launcher framestats` (parse the frame rows; count
   janky frames, p95 total frame time, and the idle frame rate from
   the frame-count delta over the idle window), `dumpsys meminfo
   com.matrix.launcher` (Total PSS), cold start TotalTime.
   Flags: `--package` (default `com.matrix.launcher`),
   `--runs` (default 3), `--baseline` (path to a previous run's JSON;
   the script compares medians against it and reports ratios).
   Report per run and medians, plus pass/fail per gate: ≤1.5 janky
   frames per key, p95 frame ≤1.15× the reference in the same session,
   key-latency p95 <50 ms (when the instrumentation from task 3 is
   present; otherwise report "not measured"), ≤1 idle frame/s,
   PSS <182 MB, cold first frame ≤1.15× reference. Always print the
   raw numbers alongside the verdicts. Release builds only; drop the
   first cold-start run; 3-run medians.
   Verify: `python3 -m py_compile tools/bench.py`; read the script —
   every tour step, collection command, and gate from this task is
   present. The script cannot be run here (the TV is not reachable
   from this VM); note that in CHANGES.md.

3. Key-latency instrumentation (only if the script needs it).
   Files: `app/src/main/java/com/matrix/launcher/ComposeHomeActivity.kt`
   (`LocalMatrixUiInspection` hook, :125; the activity already has a
   `dump()` override at :194).
   Change: ONLY if task 2 cannot measure key latency without it: add a
   debug-flagged recorder (off unless a debug build or an explicit
   debug flag) that timestamps key events and the next frame presented
   (Choreographer.FrameCallback), hung off the existing inspection
   hook, and expose the p50/p95 through the existing `dump()`
   override so the bench script can read it with
   `adb shell dumpsys activity`. If the implementer finds a simpler
   seam (e.g. logcat with a fixed tag), use it instead and say so in
   CHANGES.md. No behaviour change when the flag is off; no new
   permissions.
   Verify: code inspection that the recorder is inert when off and
   that nothing in the normal path reads it.

4. `tv-bench` skill.
   Files: new `~/workspace/custom-skills/tv-bench/SKILL.md` (outside
   the draft; custom skills live in `~/workspace/custom-skills/`).
   Change: document how to measure on the user's TV via adb — wake the
   TV, release build only, the tour from task 2, how to read the
   numbers (`framestats`, `meminfo`, `am start -W`), 3-run medians,
   the gates from task 2, and the standing rule that D-06/D-22-style
   items are decided by measurement, not by guessing. Include the
   measurement procedure from task 8 (focus zoom 1.015 vs 1.0 via the
   theme token, backdrop crossfade per focus move).
   Verify: read the skill — every step above is present and the
   commands match `tools/bench.py`.

5. Theme tokens.
   Files: new `app/src/main/java/com/matrix/launcher/MatrixTheme.kt`;
   `app/src/main/java/com/matrix/launcher/MatrixTvTheme.kt`
   (colour scheme reads the resolved theme);
   `app/src/main/java/com/matrix/launcher/MatrixLayout.kt`
   (`panelCorner`, `cardCorner`, `gap`, `cardFocusScale` delegate to
   the theme tokens, defaults = current constants);
   `app/src/main/java/com/matrix/launcher/MatrixSettingsPages.kt`
   (new `SettingsPageDef("theme", …)`, reachable from the Appearance
   page); `app/src/main/java/com/matrix/launcher/MatrixPrefs.kt`
   (no new key needed — the file lives in filesDir; add one only if
   the implementation requires it); `app/src/main/res/values/strings.xml`
   (new strings, `settings_theme_` prefix).
   Change: `theme.json` in the app's files dir maps token names
   (`background`, `panel`, `surface`, `text`, `textSecondary`,
   `accent`, `radius`, `gap`, `focusScale`, `tileSize`) to values.
   `MatrixTheme` loads it at startup into a ThemeState (defaults from
   `MatrixLayout` + `home_design.xml`: background, panel, surface,
   text, textSecondary; `accent` defaults to the text colour and maps
   to the scheme's `primary`; `tileSize` maps to `LIST_COLUMNS`,
   range 3–6). Validation: colours must parse as #RRGGBB/#AARRGGBB;
   radius 0–32dp; gap 0–32dp; focusScale 1.0–1.08; tileSize 3–6;
   text/background contrast ≥4.5, textSecondary/background ≥3.0. On
   any failure the whole file is ignored (fall back to defaults) and
   the reason is recorded. The theme applies at startup; the Theme
   page offers Apply (write + recreate the activity), Reset to
   default (delete the file + recreate), and Import (SAF open
   document, copy to filesDir/theme.json, validate, recreate). The
   page shows the resolved tokens as plain-text status rows (the
   `isStatus` pattern from the AOT layout) and, when a file was
   ignored, the reason.
   Verify: JVM-only unit test in a new
   `app/src/test/java/com/matrix/launcher/MatrixThemeTest.kt`:
   a valid file parses, a bad colour falls back, an out-of-range
   radius is rejected, low contrast is rejected, a missing file
   yields defaults. The parser/validator must be pure (String in,
   ThemeState out, no Context) so it is JVM-testable.

6. Settings backup export/import.
   Files: new `app/src/main/java/com/matrix/launcher/MatrixBackup.kt`;
   `app/src/main/java/com/matrix/launcher/MatrixSettingsPages.kt`
   (new `SettingsPageDef("backup", …)`, reachable from the Settings
   root page); `app/src/main/res/values/strings.xml`
   (`settings_backup_` prefix).
   Change: versioned JSON (`{"format":"matrix-backup","version":1,
   "prefs":{…},"theme":{…}|null}`). Export writes every entry of the
   "personal_home" SharedPreferences EXCEPT secrets (any key starting
   with `ai_api_key`, or containing `token`, `password`, or `secret`)
   plus the theme.json contents when present. Import validates
   format/version, writes the prefs (never importing a secret key even
   if one is present in the file), writes theme.json when present,
   then recreates the activity and reports how many settings were
   restored. Transport is the Storage Access Framework
   (ACTION_CREATE_DOCUMENT for export, ACTION_OPEN_DOCUMENT for
   import, MIME application/json, default name
   `matrix-backup-YYYYMMDD.json`); no new permissions. Wire the SAF
   intents through the existing settings-page scope (add a launcher
   field if none exists).
   Verify: JVM-only unit test in a new
   `app/src/test/java/com/matrix/launcher/MatrixBackupTest.kt`:
   encode/decode round-trip, secrets excluded, bad version rejected,
   missing theme handled. The codec must be pure (String in/out, no
   Context) so it is JVM-testable.

7. Safe mode / fallback Home.
   Files: new `app/src/main/java/com/matrix/launcher/MatrixSafeMode.kt`;
   `app/src/main/java/com/matrix/launcher/MatrixPrefs.kt` (new keys
   `SAFE_MODE = "safe_mode"`, `SAFE_MODE_CRASHES =
   "safe_mode_crashes"`); `app/src/main/java/com/matrix/launcher/ComposeHomeActivity.kt`
   (compute the flag in `onCreate` before `setContent`, increment the
   crash counter with `commit()` before `setContent`, reset it after
   the first successful composition); `app/src/main/java/com/matrix/launcher/ComposeHomeRepository.kt`
   (in safe mode, `load()` skips SIMKL sections/pins and custom lists
   and the wallpaper refresh returns none); `app/src/main/java/com/matrix/launcher/MatrixAiClient.kt`
   (`enabled` returns false when safe mode is on); `app/src/main/java/com/matrix/launcher/MatrixSettingsPages.kt`
   (a "Safe mode" status row on the Settings root page when active,
   with an "Exit safe mode" action, and the toggle); `app/src/main/res/values/strings.xml`
   (`settings_safe_mode_` prefix).
   Change: safe mode is on when any of these holds — (a) the settings
   toggle (`safe_mode` pref), (b) the launching intent carries the
   `safe_mode` boolean extra (adb-triggerable, per-launch, not
   persisted), (c) the crash counter reaches 3 at startup (the counter
   is incremented on every `onCreate` before `setContent` and reset
   after the first successful composition; reaching 3 sets
   `safe_mode` true and resets the counter, so it enters safe mode
   once and the user can exit via the toggle). When on: wallpaper off
   (plain background), AI discovery off, custom rows off (only app
   channels + favorites), default layout (`LIST_COLUMNS` default).
   The normal path is untouched when safe mode is off.
   Verify: JVM-only unit test for the flag/counter transition (3
   crashes → safe mode) using a map-backed SharedPreferences fake,
   if a seam exists; otherwise verify by inspection and say so in
   CHANGES.md. Code review that every gating point reads the same
   flag and that the normal path is unchanged when it is off.
   Note: `ComposeHomeRepository.kt` and `MatrixAiClient.kt` in this
   draft are main's versions (see proposal.md); the perf-refresh
   edits to them must be preserved at rebase time.

8. Measurement procedure for focus zoom and backdrop crossfade.
   Files: `~/workspace/custom-skills/tv-bench/SKILL.md` (task 4's
   skill — add a section, do not create a second skill).
   Change: document how to compare `cardFocusScale` 1.015 vs 1.0
   (set the `focusScale` token to 1.0 in theme.json — no code change
   needed) and the 80 ms-debounced backdrop crossfade per focus move,
   using the bench from task 2: same tour, same gates, 3-run medians,
   compare janky frames per key and p95 frame time. Change nothing
   in the app for this task.
   Verify: read the skill — the procedure is present and names the
   exact token and the exact numbers to compare.

9. Docs and skill hygiene.
   Files: `~/workspace/custom-skills/android-tv-compose-optimization/SKILL.md`,
   `~/workspace/custom-skills/android-dev-flow/SKILL.md`,
   `~/workspace/custom-skills/android-testing/SKILL.md` (direct edits
   — custom skills live outside the draft); new
   `~/workspace/custom-skills/tv-smoke-test/SKILL.md`; new
   `docs/tv-gotchas.md` in the draft (repo docs).
   Change: (a) fix the stale claims in the three existing skills —
   replace "this VM has no Java, Gradle, adb, sdkmanager, or Android
   SDK installed" with the actual toolchain (JDK 17 Temurin at
   `~/workspace/android-tooling/jdk-17.0.21+9`, Android SDK at
   `~/Android/sdk`, Gradle 8.13 at
   `~/workspace/android-tooling/gradle-8.13`, `GRADLE_USER_HOME` at
   `~/workspace/android-tooling/gradle-home`, `JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true"`,
   proxy flags `-Dhttp.proxyHost=hatch-egress-proxy
   -Dhttp.proxyPort=3128`); in `android-testing`, update the baseline
   to 35 test files / 178 tests (148 app + 30 core), note tests have
   run here (BUILD SUCCESSFUL), replace the MockK/Turbine
   recommendation with the repo's actual proxy-based fakes (no MockK),
   and state the test commands as `:app:testDebugUnitTest` +
   `:core:test` (the root `testDebugUnitTest` task does not exist for
   `:core`). (b) `tv-smoke-test` skill: a D-pad walkthrough on the
   real TV — install the APK (sideload steps incl. Play Protect
   "Install anyway" and "Install unknown apps"), navigate every
   settings page, verify focus order and per-row focus restoration,
   verify the AOT plain-text rows are skipped by D-pad focus, verify
   the AI key is masked, verify persistence across restart, verify
   the Refresh button, and the recovery path if the launcher breaks
   (safe mode from task 7, uninstall, clear data). (c)
   `docs/tv-gotchas.md`: TV-specific traps (other_tcp/IPv4 Gradle
   daemon loopback, debug-key install conflicts, Play Protect
   sideload blocking, key mapper / Home helper setup, release key
   backup, gradlew executable bit, workflow_dispatch runs reporting
   head_branch = main) and the "numbers first" workflow note (every
   "feels slow" becomes a bench result; each trap goes into this doc
   and the skill).
   Verify: read the three edited skills — no stale claim remains;
   read the new skill and the gotchas doc — every item above is
   present.

10. Final verification (main agent, after the user reviews the diff):
    `:app:compileDebugKotlin`, `testDebugUnitTest`, `:core:test`,
    `lint`, then PR and CI. Nothing is pushed or compiled before the
    diff review.
