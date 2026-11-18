# Update reliability

## MODIFIED Requirements

### Requirement: Update rows retain focusable identity (F06)
The update and notes rows SHALL have stable IDs. Busy update actions SHALL remain
attached focus targets and consume activation. Label changes SHALL not reset menu
focus. Installed-version display SHALL be remembered outside repeated composition.

#### Scenario: Check to download to install
- WHEN the update state changes
- THEN the same update row remains the D-pad target, including Checking/Installing.

### Requirement: Installer operations own sessions and retries (F04, F07, F08, F09)
Settings/confirmation launch failures SHALL become recoverable errors. Every created
session SHALL be abandoned on pre-commit failure, including open failure, with
cleanup exceptions isolated and Session.fsync used. Callback identity SHALL include
session and version/file metadata plus a controller operation ID. Pending session
identity SHALL persist; a subsequent attempt SHALL settle the previous session
before creating another or falling back. Terminal outcomes SHALL detach listeners.
All synchronous/asynchronous launch errors SHALL retain the verified release/file.

#### Scenario: Reordered callbacks
- WHEN an old attempt fails after a replacement starts
- THEN it cannot change the replacement controller's state.

#### Scenario: Permission return
- WHEN permission is denied or later granted
- THEN the verified file remains available and return offers Install without auto-install.

### Requirement: Metadata is strictly typed (F25)
Text fields SHALL be JSON strings. versionCode SHALL be positive, integral and
Long-range; decimal integer strings remain supported. APK URLs SHALL parse as
HTTPS with a nonempty host and no embedded credentials.

#### Scenario: Malformed metadata
- WHEN metadata contains fractional/overflow codes, null/object text or malformed HTTPS
- THEN it is rejected before offering a download.

### Requirement: A download owns cleanup through hashing and UI handoff (F31)
Downloads SHALL use distinct attempt files under updates/, serialize transfer work,
cap streamed bytes, and check cancellation during hash reads and before publication.
Every failure/cancel SHALL delete only its owned file. A canceled UI handoff SHALL
also release its completed file. SHA-256 remains the integrity gate; Content-Length
is optional progress data, not an integrity authority.

#### Scenario: Cancel after last network byte
- WHEN cancellation occurs at EOF, during hashing, or before Ready publication
- THEN the attempt cannot publish Ready and its APK is removed.

## ADDED Requirements

### Requirement: Update orchestration has deterministic regression coverage (F29)
Fake connections and installer/backend adapters SHALL exercise HTTP failures,
redirect bounds/missing Location, failed/short reads, missing length, byte cap,
hash mismatch, cancellation, retry, permission return, session failures, reordered
callbacks and disposal. FileProvider grants and actual confirmation/D-pad behavior
SHALL additionally require Android/TV evidence; JVM results SHALL not imply it.
