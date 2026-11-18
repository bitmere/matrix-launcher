# Matrix Launcher Compose 1.1.0

Historical stable release, superseded by [1.2.0](RELEASE-1.2.md). Released 2026-11-04. VersionCode 85; optimized APK 1,776,362 bytes (about 1.69 MiB); Android 8.0/API 26 or newer. Independent Kotlin/Compose edition with TV Material 3 and the retained Apple-style Home. The repository stays private.

## Included

- Favorite dock, app library, reorder controls, app-published channels/Watch Next, optional SIMKL custom lists and per-row sizing.
- One-screen SIMKL/TMDB search with type/year/available ratings and separate system voice entry. Providers appear only when configured and enabled.
- Global warm Nuvio detail handoff across Search, SIMKL and published detail cards. Native Continue Watching episode/resume fields stay intact.
- Asynchronous remote configuration with lifecycle guards, bounded atomic SIMKL disk snapshots, protected metadata/library/pinned previews and small geometry reuse caches.
- Optional Commons landscapes/local images, input popup, shared settings controls, Home capture/recovery and vendor-safe remote routing.

## Update and configure

Install the release APK over the existing Compose app; it retains the same package/signing identity and account/preferences data. Verify the accompanying SHA256. Do not uninstall first. Java edition data is separate. Installation does not enable integrations or change Home permission choices.

Use [Home setup](HOME-SETUP.md), [search setup](SEARCH.md), [SIMKL text entry](API-ENTRY.md), [wallpapers](WALLPAPERS.md) and [testing](../TESTING.md). The APK is minified/resource-shrunk; no new runtime dependency or permission was added in the final fixes.

## Evidence and limits

133 JVM tests pass; lint has no errors and one existing TV-orientation warning. Beta 55 reboot recovery and real Home/Inputs/Settings were verified. Beta 56 actual-card tests verified warm movie/series and SIMKL-to-Search switching. [Validation](VALIDATION.md) records exact versions, hashes and test limitations; the exact stable APK is installed and checksum-verified, with a separate brief dump-based Home/navigation check.

- Some navigation/frame spikes remain; measured warm improvements do not certify uninterrupted 60 fps or a causal RAM reduction.
- TCL Home key filtering can reduce held-volume repeats to one step. Keep the documented Home-mode tradeoff.
- Nuvio compatibility is verified against 1.1.0-beta.10. Future intent-contract changes require retesting. Continue Watching full playback was not certified by the final detail-only tests.
- TMDB live credential integration remains unverified on the test TV. App channels depend on what their publishers expose; Home reload cannot force another app's profile sync.
- Direct Stremio/Nuvio add-on catalog fetching is not implemented. SIMKL and both search providers remain optional.

## Next versions

This release is the baseline. Patch work starts as 1.1.1-beta.N, then 1.1.1; a feature release uses 1.2.0-beta.N, then 1.2.0. Each installable artifact increments versionCode. Publication requires an explicit user request.
