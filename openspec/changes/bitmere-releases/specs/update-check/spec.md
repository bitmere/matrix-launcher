# Update check

## ADDED Requirements

### Requirement: Read the latest stable GitHub Release

The manual check SHALL read `https://api.github.com/repos/bitmere/matrix-launcher/releases/latest` without credentials. It SHALL validate the stable tag, release URL, draft and prerelease flags, one terminal `Android-Version-Code: N` line, and the named APK and checksum assets. It SHALL compare the release code with the installed version code.

#### Scenario: Stable update is available

- GIVEN a valid published stable release with a higher version code
- WHEN the user checks for updates
- THEN Matrix offers the update and displays the reviewed notes without the machine trailer.

### Requirement: Verify the APK before installation

Matrix SHALL read `Matrix-Launcher.apk.sha256` first, verify the exact published size and SHA-256 of `Matrix-Launcher.apk`, and allow only bounded HTTPS redirects to `release-assets.githubusercontent.com`. Before installing, it SHALL verify the archive's package, version name, version code, non-debuggable flags, and exact bitmere signer set. The installed signer set SHALL also equal the approved set.

#### Scenario: Verification fails

- GIVEN a truncated download, wrong digest or an unapproved signer
- WHEN Matrix verifies the update
- THEN it SHALL refuse the install.
