# Tasks and verification record

## Implementation audit

Every finding was read and checked against the current sources, including the
partial fixes left by earlier runs. “Fixed” below means the source correction is
present and reviewed; it does not imply a successful Kotlin compile or TV pass.
No finding was dismissed as invalid.

| Finding | Status | Implementation / evidence |
| --- | --- | --- |
| F01 | Fixed | BackupSchema exact types/ranges and allowlist; MatrixBackup validates all entries before apply; strict typed preference fake and apply regression. |
| F02 | Fixed | AI and TMDB FormField.secret=true; SettingsForm remembers reveal=false state per editor; MatrixIconButton eye/eye-off toggle. Visual/semantics device check pending. |
| F03 | Fixed | MatrixBackup excludes TMDB and AI credentials on import/export. Credential regression; old-backup warning in proposal. |
| F04 | Fixed | AndroidUpdateBackend resolves/catches both settings candidates; receiver catches confirmation launch failure and retains retry/session cleanup context. |
| F05 | Fixed | openSimklListId pops the form before opening; history-level regression returns to SIMKL on Back. |
| F06 | Fixed | manual-update/manual-update-notes IDs, busy row stays focusable, MatrixMenu restores on navigation/removal; installed version loads once on IO. Physical focus check pending. |
| F07 | Fixed | Session-specific PendingIntent, persisted session/version/path match and operation listener; settle old session before fallback/new install; terminal listener cleanup and reordered callback regression. |
| F08 | Fixed | commitInstallSession owns ID before open, abandons failures, isolates cleanup errors; Session.fsync; open/write/commit failure regression. |
| F09 | Fixed | Synchronous errors preserve release/file retry context; controller regression avoids another download. |
| F10 | Fixed | 16 KiB bounded theme read/import, validation before atomic replace, asynchronous startup load/default safe theme; oversized/interrupted input regression. |
| F11 | Fixed | Backup stream byte cap and key/string/set bounds before apply; oversized collection/document regressions. |
| F12 | Fixed | Validated staged/fsynced atomic theme writes; failed editor saves do not recreate; preference commit failure explicitly reports possible partial restore in UI and tests. |
| F13 | Fixed | Actions recorded in one coroutine before async workers; Main UI progress with operation guard; bounded concurrent-tool regression. |
| F14 | Fixed | Clear cancels/invalidates search and resets busy/progress; late success/error/progress guards and generation regression. |
| F15 | Fixed | ComposeHomeRepository.searchApps is suspend with IO dispatch for catalogue miss/filter, including invalidated cache. Actual PackageManager/device path pending. |
| F16 | Fixed | 1 MiB success/error bodies, 256 models, 100 recommendations, 16 calls/four concurrent tools; transport/fan-out/recommendation regressions. |
| F17 | Fixed | Per-type library transaction gates, serialized shared activities/index refresh; real client concurrent status fixture checks one snapshot/activity fetch. Existing feed order tests retained. |
| F18 | Fixed | Activity passes startup effective safe mode to repository/AI; Home sizing/custom feeds/settings use it. Intent flag consumed so Exit can recreate normally; AI consumer regression. Full Home integration pending. |
| F19 | Fixed | TvFeed continues top-k evaluation after twelve items; late higher-weight candidate regression. |
| F20 | Fixed | NFKC Unicode letter/number normalization with original-text fallback; non-Latin regression; blank recommendation cannot match. |
| F21 | Fixed | TMDB/SIMKL filter by kind before title/year; specified years cannot use unrestricted fallback, including Cinemeta. Mixed-kind/remake regression. |
| F22 | Fixed | Provider removal deletes its provider/all model keys plus legacy fallback; other provider remains; removal regression. |
| F23 | Fixed | MatrixCardDefaults.artworkShape getter reads current radius; same-process radius regression. |
| F24 | Fixed | MatrixSaf.release checks owner and cancels pending callback; Activity destroy clears local pending result; page disposal captures owner cancellation callback. Replacement-owner regression. |
| F25 | Fixed | Exact text fields, positive integral Long/decimal-string code and parsed HTTPS host; wrong types/fraction/overflow/exponent/malformed URL regressions. |
| F26 | Fixed | Write-only resultIndex/resultsFocused and their assignments removed. |
| F27 | Fixed | Web cache key includes bounded requested count, with return limit; three/eight ordering regression. |
| F28 | Fixed | Structure guard parses assignment/Groovy namespace, indexes declared classes and explicitly permits FileProvider. Three Python fixtures and full guard pass. |
| F29 | Not fully fixed | JVM transport/session/controller tests added, but Gradle cannot start. Actual FileProvider grants/pending-confirmation Android tests and TV focus evidence remain outstanding; no reachable TV in supplied environment. |
| F30 | Not fully fixed | Strict preference-apply, safe-mode AI consumer and actual concurrent SIMKL-client fixtures added. Real Home/volume, account cancel/resume and AOT authorization/failure integration/device evidence remains outstanding; no reachable TV and JVM build unavailable. |
| F31 | Fixed | Distinct attempt-owned files, serialized transfer, hash-loop cancellation and completed-file cleanup on canceled UI handoff; EOF/hash cancellation regressions. |

## Validation

- [x] Read brief, complete audit, house rules and all six supplied skills.
- [x] Verify each finding against actual current source and retain existing functionality/provenance.
- [x] Create proposal and delta specifications before final validation.
- [x] Toolchain checks: Temurin Java 17.0.21+9 and adb 37.0.3 report versions.
- [ ] Gradle version/focused/full JVM execution: each fails before configuration/compilation with `Could not initialize native services` / `Failed to load native library 'libnative-platform.so' for Linux amd64`. No sandbox/environment workaround attempted.
- [x] `PYTHONDONTWRITEBYTECODE=1 python3 tools/audit_structure.py`: passed; 17 manifest components, Android-free core, shared TV controls guard.
- [x] `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -p 'test_audit_structure.py' -v`: 3 tests passed.
- [ ] Lint, release build, Preview, device tests and benchmarks: not run. No final optimized artifact or installation requested/produced; no version bump.

Focused JVM command used the brief's JAVA_HOME, ANDROID_HOME, GRADLE_USER_HOME,
IPv4 and proxy settings with `:app:testDebugUnitTest --tests
com.matrix.launcher.AuditRegressionTest --tests com.matrix.launcher.UpdateFlowTest
--tests com.matrix.launcher.SimklConcurrentClientTest --tests
com.matrix.launcher.AiBoundsTest`. The full command was exactly the brief's
`testDebugUnitTest :core:test` invocation. Neither reached any test or compiler task;
there are no claimed JVM passes. Final source inventory: 44 JUnit test files / 254
@Test methods, compared with 42 files / 244 methods when this run began (earlier
partial-run tests included). Inventory counts are not execution results.

## Required device follow-up

- [ ] Open AI/TMDB editors with fixture keys: XML semantics masked initially; reveal/hide with D-pad; reopen hidden. Inspect --matrix-ui IconLayout after settling and screenshot actual glyph centering.
- [ ] Follow update row focus through checking, cancel, ready, settings deny/grant/return, confirmation failure/retry. Inspect the actual TCL dialog XML, not underlying Home.
- [ ] Verify FileProvider read grant and pending-user-action confirmation using an exact measured optimized artifact; reordered/failed installs must retain retry context.
- [ ] Intent-only safe mode: no SIMKL custom rows or AI, default columns, correct settings state; Exit returns to normal. Exercise Home/volume/helper lifecycle without takeover.
- [ ] Account connect cancel/resume and concurrent library refresh using a test account; verify request counts and cache ordering against real endpoints.
- [ ] Exercise authorized/denied/failed AOT paths. Run the prescribed TV smoke tour and three-run benchmark before making performance claims.

## Files changed in this run

Production under `app/src/main/java/com/matrix/launcher/`:
- ApkDownload.kt, ApkInstaller.kt
- ComposeHomeActivity.kt
- MatrixAccountScreen.kt
- MatrixAiTools.kt, MatrixAiTransport.kt
- MatrixBackup.kt
- MatrixSearchRepository.kt, MatrixSearchScreen.kt
- MatrixSettingsPages.kt
- SimklClient.java
- UpdateCheck.kt, UpdateCheckController.kt, UpdateInstallResultReceiver.kt

Resource: `app/src/main/res/values/strings.xml`.

Tests under `app/src/test/java/com/matrix/launcher/`:
- AiBoundsTest.kt (new), SimklConcurrentClientTest.kt (new)
- AuditRegressionTest.kt, MatrixAccountNavigationTest.kt
- TestPreferences.kt, UpdateCheckTest.kt, UpdateFlowTest.kt

Tools: `tools/audit_structure.py`, `tools/test_audit_structure.py` (new).

OpenSpec: this tasks.md, proposal.md, specs/settings-safety.md,
specs/update-reliability.md, specs/search-home.md (all new).

The repository has no usable Git metadata, so this is an explicit edit inventory,
not a claimed Git diff. Earlier-run corrections in other files were reviewed and
retained. The audit report, codex-fix-inputs, version, changelog, README, workflow,
application ID, preference keys/file name, permissions and four legacy bridges
were not edited in this run.
