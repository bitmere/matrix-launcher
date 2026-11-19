# Release notes carry the SHA-256; the release page shows only the APK

Today every Matrix release publishes two assets: `Matrix-Launcher.apk`
and a `Matrix-Launcher.apk.sha256` sidecar, because the in-app updater
downloads the sidecar to learn the expected hash before installing.
The release page should show only the APK, like Nuvio, NuvioTVLauncher
and NewPipe: the SHA-256 moves into the release notes body in a stable,
parseable format, and the updater reads it from the notes.

The notes line is `SHA-256: <64 lowercase hex>` on its own line,
immediately before the terminal `Android-Version-Code:` trailer. The
parser is strict: exactly 64 lowercase hex characters in exactly that
position; anything else that looks like a hash line is a hard error,
never a silent accept.

Fallback keeps old releases installable: if the notes carry no hash
line, the updater falls back to the `.sha256` sidecar asset when one is
present — so updates to already-published releases (2.7.6, whose notes
have no hash line) still work. If neither source exists, the download
step fails instead of offering an unverifiable update.

The release workflow stops uploading the sidecar, so new release pages
show only the APK. The release gate verifies the notes contain the hash
and that it matches the built APK, and the anonymous/read-back
verification checks the notes hash and the APK bytes instead of a
sidecar. The install path is unchanged: the file hash and size are
verified in `ApkDownload.download`, and `ApkInstaller.matchesRelease`
still verifies package name, version, non-debuggable build, and the
approved signer fingerprint at install time.

No version bump, no changelog entry, no dependency change. Source,
workflow, gate and tests only; the updater's download and install
paths are untouched apart from where the expected hash comes from.
