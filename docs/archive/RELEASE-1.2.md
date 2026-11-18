# Matrix Launcher Compose 1.2.0

Stable release dated 2026-11-04. Android 8.0/API 26 or newer. Finalizes the tested beta 1–7 changes without new feature behavior.

## Changes

- Import wallpapers using remote-friendly album menus and shared image cards. The native picker opened on TCL but did not support usable D-pad selection; Matrix now handles navigation itself.
- Browse downloaded images, import through Open with/Share, and select saved wallpapers. Image-only access is requested in context; no all-files, video or write permission. Existing backgrounds are preserved.
- Choose Automatic, Nuvio or Stremio for SIMKL/search titles. Native Continue Watching cards keep their publisher’s context. Resolve catalog IDs on demand and handle warm title changes without force-stopping apps.
- Unify settings action typography and controls, including Reload Home.
- Move implementation/resources to the Matrix namespace while retaining the installed application ID, four minimal service bridges and Home alias for upgrade compatibility. Preserve upstream licenses and credits.

## Update and validation

Install over the existing Compose app; do not uninstall first. The package/signing identity, accounts, favorites, wallpaper selection and Home-service identities are retained. Shared Java edition data is separate. Photo access is requested only when importing device pictures. No new runtime dependency or permission is added beyond beta 7.

144 JVM tests pass; lint has zero errors and one existing TV-orientation warning. Stable installation verified its checksum and retained Home/account/service state. After the user left the remote idle, final stable Home/favorites D-pad, wallpaper albums and Back checks passed; automatic input stopped on Home. Beta 7 D-pad album/grid/Back and temporary-image import checks passed, alongside earlier media-routing and physical Home/Inputs/Settings checks. [Current validation](../VALIDATION.md) records the exact stable artifact and device results.

## Artifact

- VersionCode: 93
- APK: Matrix-Launcher-v1.2.0.apk
- Size: 1,793,694 bytes (about 1.71 MiB)
- SHA256: `f6791636d2e700bf9ed70100b4e189700a94a68a4ed13256512f517c47a8ad30`

The optimized, resource-shrunk release APK is installed with matching SHA256. The same bytes and checksum are attached to the stable release.

## Limits

The local browser shows up to 500 recent indexed images and copies selected images into the existing 20-import collection. Unindexed downloads can use file-manager Open with/Share. TV-specific permission and media-provider behavior can vary. On this TCL, Home key filtering retains the known held-volume repeat tradeoff. SIMKL AI search remains deferred until a documented API exists; normal search is unchanged. Direct add-on catalogs remain researched, not implemented. No YouTube integration changes are included.

Accounts, device diagnostics, images and credentials are excluded from GitHub. See [wallpapers](../WALLPAPERS.md), [media apps](../MEDIA-APPS.md), [namespace compatibility](../NAMESPACE.md) and [validation](../VALIDATION.md).

## Future versions

Patch development uses 1.2.1-beta.N before stable 1.2.1; the next feature release uses 1.3.0-beta.N before 1.3.0. Every installed artifact increments versionCode. Publishing requires an explicit user request.
