# Install (new)

## ADDED Requirements

### Requirement: Install goes through a PackageInstaller session
The install SHALL create a `PackageInstaller` session with
`SessionParams(MODE_FULL_INSTALL)`, `setAppPackageName` set to the
app's own package, `setSize` set to the verified APK's length, write
the APK as `base.apk`, `fsync` and close the stream, then
`session.commit()` with a `PendingIntent` to the result receiver. The
session SHALL be closed or abandoned on any failure before commit.

#### Scenario: Session install starts
- WHEN the user taps Install with a verified APK
- THEN a session is created with the own package name and the APK is
  staged and committed.

#### Scenario: Staged APK package mismatches
- WHEN the staged APK's package name differs from `setAppPackageName`
- THEN the install fails (per the platform rule) and the UI reports
  an error; no partial install is left behind.

### Requirement: The confirmation screen is the designed fallback, not a failure
On `STATUS_PENDING_USER_ACTION` the receiver SHALL launch the activity
in `Intent.EXTRA_INTENT` with `FLAG_ACTIVITY_NEW_TASK` immediately
(the user is actively in Settings) and the UI SHALL show "waiting for
confirmation". On `STATUS_SUCCESS` the UI SHALL show installed and the
APK SHALL be deleted. Any other status SHALL show an error with the
plain `EXTRA_STATUS_MESSAGE` and SHALL keep the verified APK so the
user can retry Install without re-downloading.

#### Scenario: System wants confirmation
- WHEN the commit result is `STATUS_PENDING_USER_ACTION`
- THEN the system confirmation screen is launched and the UI waits
  for its outcome.

#### Scenario: Install succeeds
- WHEN the commit result is `STATUS_SUCCESS`
- THEN the UI reports the update installed and the APK file is
  deleted.

#### Scenario: Install fails
- WHEN the commit result is any failure status
- THEN the UI reports the failure in plain language and the verified
  APK is kept for a retry.

### Requirement: Silent install is best-effort on API 31+ and never assumed
On API 31+ the session SHALL call
`setRequireUserAction(USER_ACTION_NOT_REQUIRED)` and the manifest
SHALL declare `UPDATE_PACKAGES_WITHOUT_USER_ACTION`. The code SHALL
NOT assume the install will be silent: it SHALL implement the
`STATUS_PENDING_USER_ACTION` path above on every API level, and SHALL
treat silent install as a possible outcome, not a promised one. On
API 26–30 the session SHALL NOT request no-user-action, and every
install there shows the system confirmation screen.

Verified basis (developer.android.com, read 2026-11-14):
`UPDATE_PACKAGES_WITHOUT_USER_ACTION` is protection level **normal**,
not `signature|privileged`, so a normal sideloaded app can declare and
hold it. `SessionParams.setRequireUserAction` grants no-user-action
only when all of: `USER_ACTION_NOT_REQUIRED` is set; the installed app
targets API 29+ on Android S (31), 30+ on T (33), 31+ on U (34), 33+
on V (35), 34+ on B (36), 35+ on C (37); the installer is the update
owner / installer of record / updating itself; and the installer
declares the permission. Matrix (targetSdk 34, updating itself) meets
the table for Android S through B but **not** Android C (needs 35+).
The docs do **not** state whether a failed no-user-action request
degrades to `STATUS_PENDING_USER_ACTION` or fails outright — the code
handles both, and on-TV verification of the real behaviour is a
release-check item (see proposal, open question 1).

#### Scenario: API 31+ self-update
- WHEN the device is API 31+ and all silent-install conditions hold
- THEN the install MAY complete with no confirmation screen.

#### Scenario: API 31+ but a condition fails, or API 26–30
- WHEN any silent-install condition fails, or the device is API 26–30
- THEN the system confirmation screen is shown and the install
  proceeds only if the user confirms.

### Requirement: The status PendingIntent is mutable
The commit's status `PendingIntent` SHALL be created mutable
(`FLAG_MUTABLE`, with `FLAG_UPDATE_CURRENT`). The platform throws
`IllegalArgumentException` for an immutable status receiver once the
caller targets API 35+; Matrix targets 34 today, but the mutable flag
is set now so a future target bump does not break installs.

### Requirement: Update ownership is never requested
The code SHALL NOT call `SessionParams.setRequestUpdateOwnership(true)`
and SHALL NOT declare `ENFORCE_UPDATE_OWNERSHIP`. Update-ownership
enforcement can only be enabled on initial install and would force
other installers through user action.

### Requirement: Unknown-sources gate runs before any install
Before creating a session, the app SHALL check
`PackageManager.canRequestPackageInstalls()`. If it returns false, no
session SHALL be created and the UI SHALL offer "Open settings", which
fires `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` with the app's
package URI (falling back to `Settings.ACTION_SECURITY_SETTINGS` if
the first intent does not resolve). When the user returns, the app
SHALL re-check the grant and resume the pending install prompt; it
SHALL NOT auto-install.

#### Scenario: Unknown-sources not allowed
- WHEN `canRequestPackageInstalls()` is false and the user taps Install
- THEN no session is created and the UI offers to open the
  install-unknown-apps settings for Matrix.

#### Scenario: User returns from settings
- WHEN the user comes back from the settings screen
- THEN the grant is re-checked and, if allowed, the install prompt
  is offered again (not started on its own).

### Requirement: System-installer fallback via FileProvider
If session creation or commit throws, the app SHALL fall back to an
`ACTION_VIEW` intent, type `application/vnd.android.package-archive`,
exposing the verified APK through the `FileProvider`
(`${applicationId}.fileprovider`, `files-path` under `updates/`) with
`FLAG_GRANT_READ_URI_PERMISSION`. The fallback SHALL still require
the unknown-sources grant; if that is missing, the unknown-sources
flow above is shown instead.

### Requirement: Manifest declares the install permissions, receiver, and provider
The app manifest SHALL declare `REQUEST_INSTALL_PACKAGES` and
`UPDATE_PACKAGES_WITHOUT_USER_ACTION`, the install-result receiver
with `android:exported="false"`, and the `FileProvider` with
`android:exported="false"` and `android:grantUriPermissions="true"`.
No other new permission SHALL be added.

### Requirement: The OS same-key rule is relied on, not reimplemented
The design relies on the platform rule that a session's existing and
new packages must have "identical package names, version codes, and
signing certificates" — an APK signed with a different key cannot
replace the installed app. The app SHALL NOT implement its own
signature check; the SHA-256 check (download-verify spec) plus the
OS key rule are the two layers.
