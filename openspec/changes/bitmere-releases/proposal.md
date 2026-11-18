# Publish Matrix updates through bitmere GitHub Releases

## Why

The updater and publisher need a single public release source that binds a reviewed version to the exact APK distributed to TVs.

## What changes

- Read stable release metadata and assets from bitmere GitHub Releases.
- Recover previously uploaded APK bytes, validate the release artifact, and verify both stored and public assets.
- Move the release application ID to `com.matrix.launcher` and require a manual clean install for the first bitmere-signed build.

## Impact

Affected components include Settings → About, APK downloading and installation, the release workflow and gate, and package-aware device tooling. This change supersedes manual-update-check's original distribution design.
