# Matrix Launcher Compose 1.1.1-beta.7

Historical prerelease, superseded by [stable 1.2.0](RELEASE-1.2.md). It contains the changes tested since stable 1.1.0.

## Changes

- Import wallpapers using remote-friendly album menus and shared image cards. The native picker opened on TCL but did not support usable D-pad selection; Matrix now handles navigation itself.
- Browse downloaded images, import through Open with/Share, and select saved wallpapers. Image-only access is requested in context; no all-files, video or write permission. Existing backgrounds are preserved.
- Choose Automatic, Nuvio or Stremio for SIMKL/search titles. Native Continue Watching cards keep their publisher’s context. Resolve catalog IDs on demand and handle warm title changes without force-stopping apps.
- Unify settings action typography and controls, including Reload Home.
- Move implementation/resources to the Matrix namespace while retaining the installed application ID, four minimal service bridges and Home alias for upgrade compatibility. Preserve upstream licenses and credits.

## Validation and artifact

144 JVM tests pass. Android lint: zero errors, one existing TV-orientation warning. Structure and tool checks pass. The exact optimized APK was installed and checksum-verified on TCL C755 / Android 14.

D-pad checks covered album selection, grid movement, Downloader, Back and temporary-image import; test files were removed and the previous wallpaper retained. Earlier previews verified both media targets and real Home/Inputs/Settings. No new real-button claim is made for beta 7.

- VersionCode: 92
- APK: Matrix-Launcher-v1.1.1-beta.7.apk
- Size: 1,793,698 bytes (about 1.71 MiB)
- SHA256: `10ff0e6bcc532fd4cd44c4085eecf28a04f8c45711c2d85a91a7c510ec71712f`

## Limits

The local browser shows up to 500 recent indexed images and copies selected images into the existing 20-import collection. Unindexed downloads can use file-manager Open with/Share. TV-specific permission and media-provider behavior can vary. On this TCL, Home key filtering retains the known held-volume repeat tradeoff. SIMKL AI search remains deferred until a documented API exists; normal search is unchanged. Direct add-on catalogs remain researched, not implemented. No YouTube integration changes are included.

Accounts, device diagnostics, images and credentials are excluded from GitHub. See [wallpapers](WALLPAPERS.md), [media apps](MEDIA-APPS.md), [namespace compatibility](NAMESPACE.md) and [validation](VALIDATION.md).
