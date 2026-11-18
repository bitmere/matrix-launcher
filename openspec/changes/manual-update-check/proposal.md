# Manual in-app update check

> **SUPERSEDED** by [bitmere-releases](../bitmere-releases/proposal.md). This proposal records the earlier updater design.

## Why

Matrix is sideloaded from GitHub Releases — there is no store to tell the
user a new version exists. Today the user has to hear about a release
elsewhere and install it by hand. This change adds one manual check:
Settings → About → "Check for updates". The app fetches a small public
`latest.json` from the release mirror, compares versions, downloads the APK
with progress, verifies its SHA-256, and installs it through a
`PackageInstaller` session. Nothing runs on launch, nothing runs in the
background, and Home is never blocked or bannered.

The private main repo stays private. The check reads only the public mirror
at `https://github.com/example/archived-release-feed`, which
publishes exactly this shape at
`https://raw.githubusercontent.com/example/archived-release-feed/main/latest.json`:

```json
{"versionName": "2.6.0", "versionCode": 148, "apkUrl": "https://raw.githubusercontent.com/example/archived-release-feed/main/Matrix-Launcher-v2.6.0.apk", "sha256": "3a6659da014385b98ffc1ef244bb7da1baea61300d5e588a72004096add622ab", "notes": "..."}
```

## Scope

1. **Check.** A small hand-rolled client fetches `latest.json` with
   `HttpURLConnection` (no new dependency — the codebase already uses
   `HttpURLConnection` in `MatrixAiClient`/`MatrixAiTransport`), parses the
   five fields, and decides "newer" by integer `versionCode` comparison
   against the installed `versionCode`. `versionName` is display-only.
2. **Download + verify.** The APK streams to `filesDir/updates/` with a
   progress callback, on a background thread. Before any install is
   attempted the file is verified against the `sha256` from `latest.json`
   (SHA-256 is the gate; see the note on size below). A failed, partial, or
   mismatched download is deleted and reported as an error, never installed.
3. **Install.** A `PackageInstaller` session (`MODE_FULL_INSTALL`,
   `setAppPackageName` to the own package) writes the verified APK and
   commits with a `PendingIntent` to a non-exported result receiver. On
   API 31+ the session requests `USER_ACTION_NOT_REQUIRED`; on API 26–30
   it does not, and every install shows the system confirmation screen.
   If the system still wants a tap, the receiver launches the confirmation
   intent from `STATUS_PENDING_USER_ACTION`. The `ACTION_VIEW` +
   `FileProvider` system-installer path is the fallback if the session
   path fails.
4. **Unknown-sources gate.** Before installing, the app checks
   `PackageManager.canRequestPackageInstalls()`. If false, the UI offers
   "Open settings", which fires `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`
   with the app's package URI, and re-checks when the user comes back.
5. **UI.** One "Check for updates" row in Settings → About (the `about`
   page in `MatrixSettingsPages.kt`), built with `MatrixButton` /
   `MatrixControlStyle` and strings in resources, D-pad friendly. States:
   checking / up to date / update available (version + notes) /
   downloading (progress) / ready to install / install result / error
   with a plain message. No banner, no Home change, no check on launch.
6. **Manifest.** `REQUEST_INSTALL_PACKAGES`,
   `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, and a `FileProvider` entry
   (`${applicationId}.fileprovider`, new `res/xml/filepaths.xml` — the app
   has no `FileProvider` today). No other new permission.
7. **Tests.** Pure/JVM tests for version comparison, `latest.json`
   parsing, and SHA-256 verification. No network in tests.
8. **Release.** Ships in the next version, **2.7.0** (new feature).
   Do NOT bump `version.properties` in this change — the version bump
   travels with the release, as usual.

## Platform verdict: silent install (verified from the official docs, 2026-11-14)

The starting belief for this change was that
`android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION` is
`signature|privileged` and therefore ungrantable to a normal sideloaded
app. **That belief is wrong, and the spec is written on the corrected
verdict below.** All quotations are from developer.android.com pages read
2026-11-14 (API reference, current revision).

- **The permission is `normal`, not `signature|privileged`.**
  `Manifest.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION`:
  "Allows an application to indicate via
  `PackageInstaller.SessionParams.setRequireUserAction(int)` that user
  action should not be required for an app update. Protection level:
  normal"
  (`https://developer.android.com/reference/android/Manifest.permission`).
  A normal sideloaded app can declare it and it is granted at install.
  The permission itself is not the blocker.
- **Silent install is possible for a self-update on API 31+, but it is
  not guaranteed.** `PackageInstaller.SessionParams.setRequireUserAction`
  (`https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams`):
  "For installers that have been granted the
  `REQUEST_INSTALL_PACKAGES` permission, user action will not be
  required when all of the following conditions are met:
  - `requireUserAction` is set to `USER_ACTION_NOT_REQUIRED`.
  - The app being installed targets: API 29 or higher on Android S
    (API 31); API 30 or higher on Android T (API 33); API 31 or higher
    on Android U (API 34); API 33 or higher on Android V (API 35);
    API 34 or higher on Android B (API 36); API 35 or higher on
    Android C (API 37).
  - The installer is: the `update owner` of an existing version of the
    app … if the update ownership enforcement is enabled; the
    `installer of record` of an existing version of the app … if the
    update ownership enforcement isn't enabled; **updating itself**.
  - The installer declares the
  `UPDATE_PACKAGES_WITHOUT_USER_ACTION` permission.
  Note: The target API level requirement will advance in future Android
  versions. Session owners should always be prepared to handle
  `STATUS_PENDING_USER_ACTION`."
  Matrix is updating itself, so it meets the installer-identity
  condition on the face of the doc; it holds `REQUEST_INSTALL_PACKAGES`
  once the unknown-sources grant is in place; and with `targetSdk 34`
  it meets the target table for Android S through B (B needs exactly
  34+) but **not** Android C (needs 35+). A future `targetSdk` bump is
  needed to keep the silent path on Android C and later — record it,
  do not fix it here.
- **The confirmation screen remains the designed fallback, on every
  API level.** `PackageInstaller.STATUS_PENDING_USER_ACTION`:
  "User action is currently required to proceed. You can launch the
  intent activity described by `Intent.EXTRA_INTENT` to involve the
  user and continue. You may choose to immediately launch the intent
  if the user is actively using your app."
  (`https://developer.android.com/reference/android/content/pm/PackageInstaller`).
  The receiver MUST implement this path. On API 26–30 there is no
  silent path at all (`setRequireUserAction(int)` /
  `USER_ACTION_NOT_REQUIRED` are API 31+; the doc's target table
  starts at Android S / API 31), so every install there shows the
  confirmation screen.
- **One thing the docs do not say — recorded as an open question,
  not assumed:** the `setRequireUserAction` doc does not state what
  happens when `USER_ACTION_NOT_REQUIRED` is requested but a
  condition fails — whether the system degrades to
  `STATUS_PENDING_USER_ACTION` or fails the install outright. The
  doc only says owners should "always be prepared to handle
  `STATUS_PENDING_USER_ACTION`". The design therefore handles both:
  `STATUS_PENDING_USER_ACTION` launches the confirmation intent, and
  any failure status is reported as an error. Do not assume silent.
- **Two doc details the implementation must respect:**
  - `PackageInstaller.Session.commit(IntentSender)`:
    "Attempt to commit everything staged in this session. This may
    require user intervention… The final result of the commit will be
    reported through the given callback." It throws
    `IllegalArgumentException` "if the `statusReceiver` [is] from an
    immutable `PendingIntent` when caller has a target SDK of API
    version 35 or above" — Matrix targets 34, so it does not throw
    today, but the status `PendingIntent` MUST be created mutable
    (`FLAG_MUTABLE`) now so a future target bump does not break it.
    (`https://developer.android.com/reference/android/content/pm/PackageInstaller.Session`).
  - The `PackageInstaller` class doc says committing "may require user
    intervention … unless the caller falls into one of the following
    categories … the device owner [or] the affiliated profile owner".
    That class-level list does **not** mention the self-update path;
    the `SessionParams.setRequireUserAction` doc is the fuller
    authority. Do not "correct" this design from the class doc alone.
  - `SessionParams.setAppPackageName`: "It's strongly recommended
    that you provide this value when known… If the APKs staged in the
    session aren't consistent with this package name, the install will
    fail."
  - Do NOT call `SessionParams.setRequestUpdateOwnership(true)`: it
    "can only be enabled on initial installation", requires the
    `ENFORCE_UPDATE_OWNERSHIP` permission, and once enabled "the
    other installers will need the user action to update the package
    even if the installers have been granted the `INSTALL_PACKAGES`
    permission."
- **Unknown-sources gate (verified).**
  `PackageManager.canRequestPackageInstalls()`:
  "Checks whether the calling package is allowed to request package
  installs through package installer… If this API returns false, the
  install request will be blocked by the package installer and a
  dialog will be shown to the user with an option to launch settings
  to change their preference. An application must target Android O or
  higher and declare permission
  `Manifest.permission.REQUEST_INSTALL_PACKAGES` in order to use this
  API."
  (`https://developer.android.com/reference/android/content/pm/PackageManager`).
  `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`: "Show settings to
  allow configuration of trusted external sources. Input: Optionally,
  the Intent's data URI can specify the application package name to
  directly invoke the management GUI specific to the package name."
  (`https://developer.android.com/reference/android/provider/Settings`).
  Note: the reference page lists `REQUEST_INSTALL_PACKAGES` itself as
  "Protection level: signature"; the operative, user-facing gate is
  nevertheless the per-source unknown-sources grant surfaced by
  `canRequestPackageInstalls()` — the manifest declaration alone is
  never treated as the grant, and the check runs at install time.
- **First-silent-update caveat.** The permission only counts for the
  build that declares it. Release 2.7.0 is itself installed the old,
  confirmed way; the first update that can go silent is the one
  installed *by* 2.7.0 (i.e. 2.7.0 → next release). Do not promise
  silent install for the 2.7.0 install itself.

## Trust model

- The mirror is public and unauthenticated, so `latest.json` and the
  APK are only as trustworthy as the mirror repo and HTTPS. The app
  therefore verifies the downloaded file's SHA-256 against the hash in
  `latest.json` before installing, and the OS enforces the
  same-signing-key rule: a `PackageInstaller` session requires "all
  existing and new packages [to] have identical package names, version
  codes, and signing certificates", so an APK signed with any other
  key cannot replace the installed app. Both layers hold; neither
  replaces the other.
- Size check: the published `latest.json` shape carries **no size
  field**, so there is nothing to compare a size against. The SHA-256
  comparison is the gate. If the mirror later adds a size field, the
  downloader compares it too; the code must not invent a size from
  `latest.json` as published. (Content-Length, when the server sends
  one, may be used for progress display only.)
- No token, no secret, and no credential of any kind is embedded in
  the app or sent with the check. The mirror URL is a public constant.

## Non-goals

- No background jobs, no periodic check, no check on launch, no
  auto-install, no idle install, no banner, no "updated" notice.
- No beta channel, no channel picker — stable only, one universal APK,
  no ABI selection.
- No dex-metadata embedding, no baseline-profile handling.
- No copying of Nuvio code. Nuvio (`NuvioMedia/NuvioTV`) is GPL-3.0;
  Matrix is MIT. The behaviour is reimplemented from the Android
  platform APIs documented above; the pattern is portable, the code
  is not. (See `~/workspace/github-audit/nuvio-in-app-updates-20261114.md`.)
- No `UPDATE_PACKAGES_WITHOUT_USER_ACTION` reliance as a guarantee:
  the design treats silent install as a best-effort path and the
  confirmation screen as the normal path.
- No change to Home, channels, search, theme, backup, or any other
  settings page. No new dependency, no DI framework, no Retrofit,
  no WorkManager / JobScheduler.
- No `version.properties` bump in this change (2.7.0 ships with the
  release, as usual).

## Open questions

1. **Unverified:** when `USER_ACTION_NOT_REQUIRED` is requested but a
   silent-install condition fails, does the system degrade to
   `STATUS_PENDING_USER_ACTION` or fail the install? The docs do not
   say. The design handles both; on-TV verification of the real
   behaviour is a release-check item, not a spec assumption.
2. **On-TV verification required:** the install-prompt and
   unknown-sources flows cannot be verified in this VM or an emulator
   with confidence — the confirmation screen is a phone-style dialog
   on TV, and Matrix is the running Home app, so installing an update
   replaces the running launcher. Verify on a real TV before release:
   confirmation screen reachable and D-pad usable; unknown-sources
   round-trip returns to the About page; a successful install restarts
   Matrix cleanly.
3. **Mirror publishing is a release-process step, not code:** each
   release must update `latest.json` (versionName, versionCode,
   apkUrl, sha256, notes) and publish the APK under the same public
   mirror. If `latest.json` is stale, the check silently reports the
   stale version — there is no way for the app to detect that.
