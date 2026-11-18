# Update check (new)

## ADDED Requirements

### Requirement: The check reads latest.json from the public release mirror
The app SHALL fetch `latest.json` from the public release mirror
(`https://raw.githubusercontent.com/example/archived-release-feed/main/latest.json`)
with `HttpURLConnection` and no new dependency, only when the user taps
"Check for updates". No check SHALL run on launch, on Home start, or when
the About page is merely opened.

#### Scenario: User taps Check for updates
- WHEN the user activates the row in Settings → About
- THEN one GET is issued to the mirror URL and the UI enters the
  checking state.

#### Scenario: App starts or Home opens
- WHEN the app launches or Home is shown
- THEN no network request for an update is issued.

### Requirement: latest.json parsing is strict about the fields that decide trust
`parseLatestJson` SHALL require `versionName`, `versionCode`, `apkUrl`,
`sha256`, and `notes`; `versionCode` SHALL be a positive integer,
`apkUrl` SHALL use `https:`, and `sha256` SHALL be 64 hex characters.
Any document failing these rules SHALL be an error, never an update.

#### Scenario: Valid document
- WHEN the document has all five fields with a positive integer
  versionCode, an https apkUrl, and a 64-hex sha256
- THEN parsing succeeds and the release is usable.

#### Scenario: Malformed document
- WHEN a field is missing, versionCode is not a positive integer, the
  apkUrl is not https, or the sha256 is not 64 hex characters
- THEN the check reports an error with a plain message and offers no
  download.

#### Scenario: Extra fields
- WHEN the document carries fields beyond the five known ones
- THEN they are ignored and parsing proceeds.

### Requirement: "Newer" means a higher versionCode
The app SHALL treat the remote release as newer exactly when
`remoteVersionCode > installedVersionCode`, compared as integers.
`versionName` SHALL never decide newness; it is display-only.

#### Scenario: Remote code higher
- WHEN remote versionCode is 149 and installed is 148
- THEN the release is an update, whatever the versionNames say.

#### Scenario: Remote code equal or lower
- WHEN remote versionCode is 148 or lower and installed is 148
- THEN the UI reports up to date.

### Requirement: Check failures are plain errors, never silent and never retried automatically
A network failure, a non-200 HTTP status, a timeout, or a parse failure
SHALL surface as an error state with a plain-language message and a
"Try again" action. The app SHALL NOT retry on its own and SHALL NOT
treat a failed check as "up to date".

#### Scenario: No network
- WHEN the fetch fails (no connectivity, timeout, HTTP error)
- THEN the UI shows an error such as "Couldn't check for updates.
  Check your connection and try again." and no download is offered.

### Requirement: No credential is ever sent or embedded
The check SHALL send no token, key, or credential of any kind, and the
mirror URL SHALL be a public constant. The check works unauthenticated
because the mirror repo is public.

## MODIFIED Requirements

### Requirement: About page shows the installed version (unchanged behaviour, kept)
The About page SHALL continue to show the installed version in its
description, as today; the update row is added alongside it and does
not replace it.
