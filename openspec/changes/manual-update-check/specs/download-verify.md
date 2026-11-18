# Download and verify (new)

## ADDED Requirements

### Requirement: The APK downloads to filesDir/updates with progress
The download SHALL stream the APK from `apkUrl` to
`filesDir/updates/Matrix-Launcher-v<versionCode>.apk` on a background
thread, reporting progress (bytes downloaded, and a percentage when
Content-Length is present). Only one download SHALL run at a time; a
second request while one is in flight SHALL return the in-flight state
instead of starting another download.

#### Scenario: Download in progress
- WHEN the download is running
- THEN the UI shows progress and offers Cancel, and Home keeps
  working normally.

#### Scenario: Download cancelled
- WHEN the user cancels
- THEN the partial file is deleted and the UI returns to the
  update-available state.

### Requirement: SHA-256 verification gates every install
After the stream finishes, the app SHALL compute SHA-256 over the
downloaded file and compare it with the `sha256` from `latest.json`.
The install SHALL proceed only on an exact match. On mismatch the file
SHALL be deleted and the UI SHALL report a verification error.

#### Scenario: Hash matches
- WHEN the computed SHA-256 equals the published hash
- THEN the UI enters the ready-to-install state.

#### Scenario: Hash mismatches
- WHEN the computed SHA-256 differs by any amount
- THEN the file is deleted, no install is attempted, and the UI
  reports a verification error.

### Requirement: latest.json carries no size, so no size gate is invented
The published `latest.json` shape has no size field, so the downloader
SHALL NOT compare the file size against `latest.json`. SHA-256 is the
gate. If the mirror later adds a size field, the downloader SHALL
compare it too. Content-Length, when the server sends one, MAY be used
for progress display only.

#### Scenario: File of unexpected size but matching hash
- WHEN the file's size differs from any expectation but its SHA-256
  matches the published hash
- THEN the install proceeds (the hash is authoritative).

### Requirement: Stale and partial files are cleaned up
Starting a new download SHALL delete APKs for other versions in
`filesDir/updates/` and any partial file from a failed or cancelled
download. The verified file SHALL be kept until the install result
arrives, then deleted on install success.

#### Scenario: New version starts downloading
- WHEN a download for versionCode 149 starts while a 148 APK is stored
- THEN the 148 file is deleted first.
