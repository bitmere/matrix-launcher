# skills-docs

## ADDED Requirements

- THE `tv-bench` SKILL SHALL document how to measure on the user's TV
  via adb: wake the TV, release build only, the tour from
  `tools/bench.py`, how to read the numbers (`framestats`, `meminfo`,
  `am start -W`), 3-run medians, the gates, and the standing rule
  that D-06/D-22-style items are decided by measurement.

- THE `tv-bench` SKILL SHALL document the measurement procedure for
  the focus zoom (1.015 vs 1.0, via the `focusScale` theme token) and
  the 80 ms-debounced backdrop crossfade per focus move, with the
  exact numbers to compare.

- THE `tv-smoke-test` SKILL SHALL document a D-pad walkthrough on the
  real TV: install (sideload steps incl. Play Protect), navigate every
  settings page, focus order and per-row focus restoration, AOT
  plain-text rows skipped by D-pad focus, AI key masked, persistence
  across restart, the Refresh button, and the recovery path (safe
  mode, uninstall, clear data).

- THE REPO SHALL gain `docs/tv-gotchas.md` recording TV-specific
  traps (other_tcp/IPv4 Gradle daemon loopback, debug-key install
  conflicts, Play Protect sideload blocking, key mapper / Home helper
  setup, release key backup, gradlew executable bit,
  workflow_dispatch runs reporting head_branch = main) and the
  "numbers first" workflow note.

## MODIFIED Requirements

- THE THREE existing custom skills (`android-tv-compose-optimization`,
  `android-dev-flow`, `android-testing`) SHALL state the actual
  toolchain (JDK 17 Temurin, Android SDK, Gradle 8.13, the IPv4 flag
  and proxy flags) instead of claiming this VM has no Java, Gradle,
  adb, sdkmanager, or Android SDK installed.

- THE `android-testing` SKILL SHALL state the current baseline (35
  test files, 178 tests: 148 app + 30 core), that tests have run here,
  that the repo uses proxy-based fakes (no MockK), and the test
  commands `:app:testDebugUnitTest` + `:core:test`.

## Non-goals

- No new skills beyond `tv-bench` and `tv-smoke-test`. No `theme`
  skill yet — it depends on the theme work landing and being used.
