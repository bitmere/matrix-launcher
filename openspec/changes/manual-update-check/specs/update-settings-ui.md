# Update settings UI (new)

## ADDED Requirements

### Requirement: One "Check for updates" row in Settings → About
The `about` page in `MatrixSettingsPages.kt` SHALL gain a "Check for
updates" row, built with `MatrixButton` / `MatrixControlStyle` and
strings in `res/values/strings.xml`, reachable and operable by D-pad
like the other About rows. No other settings page SHALL change, and
Home SHALL not change.

#### Scenario: About page opens
- WHEN the user opens Settings → About
- THEN the "Check for updates" row is present and focused normally,
  and no check has run.

### Requirement: The row covers every update state in plain language
One `UpdateUiState` SHALL drive the row and its detail text: checking
(row disabled, "Checking…"); up to date ("You're on the latest
version" + installed version); update available (remote version +
notes, "Download" action); downloading (progress %, "Cancel");
ready to install ("Install update"); install result (installed /
waiting for confirmation / error with a plain message + "Try again").
Error text SHALL be plain language, never a raw status code, URL, or
stack trace.

#### Scenario: Update available
- WHEN the check finds a higher versionCode
- THEN the row shows the remote version and its notes with a
  Download action.

#### Scenario: Check fails
- WHEN the check errors
- THEN the row shows a plain error message and a Try again action.

### Requirement: No check runs except on the row's action
The check SHALL run only when the user activates the row (or its Try
again action). Opening the About page, opening Settings, or starting
Home SHALL NOT trigger a check.

### Requirement: All update work stays off the main thread and out of composition
Network, download, hashing, and install-session work SHALL run on
`Dispatchers.IO` (or a worker scope), never on the main thread and
never inside composition. The state holder SHALL be a small plain
class owned by the About page scope (no DI framework), holding the
current `UpdateUiState` and the pending release.

### Requirement: No banner, no notice, no Home interruption
An available update SHALL NOT produce a banner, a notification, or
any Home change. The user learns about it only by opening Settings →
About and checking. Installing replaces the running launcher; the
new build SHALL start clean, and surviving a process restart mid-
install is NOT required — after an install the user re-checks if
needed.

## MODIFIED Requirements

### Requirement: About page keeps its existing rows
The existing About rows (data sources, licenses) and the installed-
version description SHALL behave exactly as before; the update row is
added alongside them.
