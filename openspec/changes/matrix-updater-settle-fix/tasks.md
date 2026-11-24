# Tasks — matrix-updater-settle-fix

- [x] Implement shared existence-aware settlement and synchronous clearing.
- [x] Update confirmation failures and installer diagnostics.
- [x] Add plain JVM tests for stale, live, raced, failed, and absent sessions.
- [x] Review the diff before compiling.
- [x] Run focused tests, then the full app JVM suite and lint sequentially.
- [x] Record verification results and commit on matrix-updater-settle-fix.

## Verification (2026-11-24, branch matrix-updater-settle-fix @ 2.7.11 main fe93a37d)

- Diff review: two points addressed — the receiver wraps the settle call
  (including store construction) in `runCatching` so obtaining the installer
  cannot crash `onReceive`; `ApkInstaller` captures the saved session id
  before the `try` so the log line itself cannot throw out of the `catch`.
- Focused tests: `UpdateSettleTest`, 10/10 pass.
- Full JVM suite: 306 tests (276 app + 30 core), 0 failures — baseline 296
  at 2.7.11 plus the 10 new tests.
- Lint (`:app:lintDebug`): 0 errors, 4 warnings, all pre-existing
  (ApplySharedPref in MatrixBackup, AGP version, UnusedResources, UseKtx
  on the untouched `save()` line).
- Note: Codex's sandboxed Gradle run failed before starting
  ("Could not determine a usable wildcard IP") and its 4.5 GB Gradle-home
  copy attempt was removed; the gates were run directly outside the sandbox.

The seam exposes existence as a Boolean to avoid constructing Android framework
objects on the JVM. Both callback settlement and preference cleanup exercise the
same throwing settlement helper. The installer error branch remains unchanged
apart from logging; invoking ApkInstaller.install requires Android Context and
package verification, so its error mapping is inspected rather than JVM-tested.
