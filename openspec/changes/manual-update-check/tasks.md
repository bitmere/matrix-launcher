# Tasks

1. Check client (pure + thin I/O split): new `UpdateCheck.kt` in
   `com.matrix.launcher` (new `update` area, plain constructor wiring, no
   DI framework): a pure `parseLatestJson(json): LatestRelease?` and a
   pure `isNewer(remoteVersionCode, installedVersionCode): Boolean`
   (`remote > installed`, integer comparison; `versionName` never
   compared), plus a thin `fetchLatest()` that GETs the mirror
   `latest.json` with `HttpURLConnection` (connect/read timeouts, e.g.
   10 s / 15 s, `User-Agent: Matrix-Launcher/<versionName>`), accepts
   only HTTP 200, and returns a typed result (`Available`, `UpToDate`,
   `Error`). No new dependency. The mirror URL is one constant.
2. Check rules: `parseLatestJson` requires all five fields
   (`versionName`, `versionCode`, `apkUrl`, `sha256`, `notes`) and
   rejects the document otherwise; `versionCode` must be a positive
   integer; `apkUrl` must be `https:`; `sha256` must be 64 lowercase
   hex chars (normalise upper-case on parse, compare lower-case).
   `notes` may be empty. Unknown extra fields are ignored. A malformed
   document is an `Error`, never an `Available`.
3. Download + verify: new `ApkDownload.kt`: stream the APK from
   `apkUrl` to `filesDir/updates/Matrix-Launcher-v<versionCode>.apk`
   with a progress callback (bytes downloaded / total when
   Content-Length is present, indeterminate otherwise), on
   `Dispatchers.IO`, cancellable, one download at a time (a second
   check/download while one runs is a no-op returning the in-flight
   state). After the stream finishes, compute SHA-256 over the file
   and compare with `latest.json`'s `sha256`; on mismatch delete the
   file and report a verification error. Delete partial files on
   failure/cancel. Delete stale APKs for other versions in
   `filesDir/updates/` when a new download starts. Keep the verified
   file until the install result arrives, then delete it on success.
4. Install session: new `ApkInstaller.kt`: `install(apkFile,
   release)`: if `!packageManager.canRequestPackageInstalls()` return
   the `NeedsUnknownSources` outcome (do not create a session). Else
   create a session with `SessionParams(MODE_FULL_INSTALL)`,
   `setAppPackageName(context.packageName)`,
   `setSize(apkFile.length())`, and on API 31+
   `setRequireUserAction(USER_ACTION_NOT_REQUIRED)`; write the APK as
   `base.apk` via `openWrite`, `fsync`, close the stream, then
   `session.commit()` with a mutable `PendingIntent` (`FLAG_MUTABLE`,
   `FLAG_UPDATE_CURRENT`) to the non-exported result receiver. Do NOT
   call `setRequestUpdateOwnership`. Close/abandon the session on any
   failure before commit.
5. Install result receiver: new non-exported `BroadcastReceiver`
   (manifest-declared, `android:exported="false"`) receiving
   `EXTRA_STATUS`: `STATUS_SUCCESS` → installed (delete the APK, show
   the installed state); `STATUS_PENDING_USER_ACTION` → launch the
   `Intent.EXTRA_INTENT` with `FLAG_ACTIVITY_NEW_TASK` immediately
   (the user is actively in Settings) and show "waiting for
   confirmation"; any other status → error with the plain
   `EXTRA_STATUS_MESSAGE` (never the raw status code alone) and keep
   the verified APK so the user can retry Install without
   re-downloading. The receiver must not assume silent install.
6. Unknown-sources flow: when the install step returns
   `NeedsUnknownSources`, the About UI shows "Allow Matrix to install
   updates" with an "Open settings" action firing
   `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` with data
   `Uri.parse("package:" + context.packageName)` (fallback
   `Settings.ACTION_SECURITY_SETTINGS` if the first intent does not
   resolve); on return to the About page, re-check
   `canRequestPackageInstalls()` and resume the pending install
   prompt (do not auto-install).
7. System-installer fallback: if session creation or commit throws
   (or the session path is unavailable), fall back to an `ACTION_VIEW`
   intent, type `application/vnd.android.package-archive`, with the
   verified APK exposed through the new `FileProvider`
   (`${applicationId}.fileprovider`, `res/xml/filepaths.xml` granting
   `files-path` under `updates/`), `FLAG_GRANT_READ_URI_PERMISSION`.
   The fallback still requires the unknown-sources grant; if that is
   missing, show the same unknown-sources flow as task 6 instead.
8. Manifest + provider: add `REQUEST_INSTALL_PACKAGES` and
   `UPDATE_PACKAGES_WITHOUT_USER_ACTION` to the app manifest, declare
   the result receiver (non-exported) and the `FileProvider` (non-
   exported, `android:grantUriPermissions="true"`). No other new
   permission.
9. UI: add a "Check for updates" row to the `about` page in
   `MatrixSettingsPages.kt` (after the existing rows), using
   `MatrixButton` / `MatrixControlStyle` and new strings in
   `res/values/strings.xml`. States, all D-pad reachable, all driven
   from one `UpdateUiState`: checking (row disabled, "Checking…") /
   up to date ("You're on the latest version" + installed version) /
   update available (remote version + notes, "Download" action) /
   downloading (progress %, "Cancel") / ready to install ("Install
   update") / install result (installed / waiting for confirmation /
   error with a plain message + "Try again"). No check runs on launch
   and no check runs when the page is merely opened — only on the
   row's action. No Home change, no banner.
10. State + wiring: one small state holder (e.g.
    `UpdateViewModel`-style plain class, no DI framework) owned by the
    About page scope, holding the current `UpdateUiState` and the
    pending `LatestRelease`; all network/download/install work on
    `Dispatchers.IO` (or a worker scope), never on the main thread and
    never in composition. Surviving a process restart is NOT required:
    if Matrix is replaced mid-install, the new build starts clean and
    the user re-checks.
11. Tests: `UpdateCheckTest` (pure/JVM, in
    `app/src/test/java/com/matrix/launcher/`): version comparison
    (remote > installed, equal, lower, large codes); `latest.json`
    parsing (valid doc, missing field, non-integer versionCode,
    non-https apkUrl, short/odd sha256, extra fields ignored);
    SHA-256 verification (matching hash passes, one-byte change fails,
    empty file fails). No network in tests. No new tests for the
    Android-only seams (session, receiver, FileProvider, unknown-
    sources round-trip) — verified by code reading and on-TV check.
12. Build + on-TV verification: full suite (`:app:testDebugUnitTest`
    + `:core:test`) plus `lint` with `--profile` in the fixes checkout;
    expect the suite green and lint 0 errors, 0 warnings. Then on a
    real TV: check with no update, with an update available, download
    progress, SHA-256 mismatch (point the mirror at a bad hash in a
    scratch copy — never publish a bad hash to the real mirror),
    unknown-sources round-trip, confirmation screen D-pad usability,
    and a successful install restarting Matrix. Do NOT bump
    `version.properties` (stays 2.6.0/148 until the 2.7.0 release).
