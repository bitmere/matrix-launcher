# minSdk assessment: API 26 to 24 or lower

Report only, for the supplied fc4d54c1 / 2.7.5 (154) source copy. No SDK values, dependencies, signing, package identity or runtime code were changed. Current configuration in [app/build.gradle](../../../app/build.gradle): minSdk 26, targetSdk 34, compileSdk 36; Java/Kotlin toolchain 17. Language bytecode compatibility does not supply missing Android/Java library APIs.

## Finding

**API 24 is feasible as a compatibility project, not a safe one-line change.** Basic apps, favorites, search and optional SIMKL could plausibly work after the changes below. Android O preview channels and Watch Next cannot retain their existing behavior on standard API 24/25 because those provider tables do not exist. Full feature parity therefore cannot be promised. Keep API 26 for the present release.

API 23 is a further possibility after additional guards and tests. Going below API 23 is incompatible with the inspected current Compose/Activity/TV Material dependencies without downgrading or replacing them. Nuvio’s minimum is not evidence that Matrix’s APIs and feed architecture work on the same devices.

## Dependency evidence

The app pins Compose BOM 2026.03.00, Activity Compose 1.12.4, TV Material 1.1.0, Lifecycle Runtime Compose 2.9.4, Coil Compose 2.7.0, ZXing 3.5.3 and dadb 1.2.10. `core` is Android-free Java and compiles against `org.json` supplied by Android in the app. No core-library desugaring dependency or enable flag is configured.

The following minimums were read directly from cached AAR **AndroidManifest.xml** files for the pinned libraries/the BOM-resolved Compose artifacts (not inferred from current website versions):

| Artifact inspected | Version | Declared minSdk |
| --- | --- | --- |
| Compose UI Android / UI Text Android / Foundation Android | 1.10.5 | 23 |
| Activity | 1.12.4 | 23 |
| TV Material | 1.1.0 | 23 |
| Lifecycle Runtime Android | 2.9.4 | 21 |
| Coil Base | 2.7.0 | 21 |

These declarations show no API-26 floor in the inspected UI libraries. They are not a fresh full resolved dependency/merged-manifest check. ZXing/dadb and their transitive bytecode still need a lower-API lint/runtime audit, especially local ADB crypto/network behavior. A future compatibility branch must resolve the complete graph and inspect the merged manifest/AAR metadata; do not force `tools:overrideLibrary` around incompatibilities. The Activity release history documents its move to API 23. [AndroidX Activity release notes](https://developer.android.com/jetpack/androidx/releases/activity).

## Concrete blockers and required work

| Area and source | Effect below 26 | Work required |
| --- | --- | --- |
| `ComposeHomeActivity.kt`, `MatrixHomeScreen.kt`, `MatrixHomeSurface.kt`, `MatrixClockScreen.kt`: `java.time.LocalDateTime` / `DateTimeFormatter`; `core/SimklRequestPolicy.java`: `Instant` / `ZoneId` | Framework `java.time` is unavailable before 26. Home clock/startup and SIMKL quota-reset paths can fail. JVM tests run on Java 17 and would not expose this. | Enable compatible core-library desugaring at the app DEX boundary, including app uses of core bytecode, or replace the time APIs. Verify America/New_York quota reset across DST, formatting and startup on 24/25. |
| `BoundedInput.kt`, `SimklDiskCache.java`: `java.nio.file.Files`, `File.toPath`, atomic/replace moves | NIO file APIs are unavailable before 26. Theme/backup imports and SIMKL cache writes can fail. | Use a desugaring configuration that covers the exact NIO APIs or add an older-platform atomic-write adapter. Verify failed writes preserve existing files, cleanup works and cache recovery is correct. Basic Java 8 language desugaring alone is insufficient. |
| `TvFeed.java`: `content://android.media.tv/preview_program` and `watch_next_program`; `HomeFeedSource.java`, `FeedRules.java` | Android 8 introduced preview-channel and Watch Next tables. Queries may fail/return no rows; existing exception/status handling is not equivalent functionality. | Gate these readers/UI on capability/version and explicitly support an apps/favorites/catalog/SIMKL-only Home on old TVs, or design an alternative legacy-recommendation backend. An older TV channel table is not a replacement for preview/Watch Next programs. No legacy reader exists here. |
| `res/mipmap-anydpi/matrix_launcher.xml` is an adaptive icon in the unqualified resource directory; API-33 variant adds monochrome | Adaptive-icon inflation is an API-26 facility; there is no inspected pre-26 launcher-icon fallback at that name. | Add legacy bitmap/vector mipmaps and qualify the adaptive version as `-v26`; verify OEM Home icon/banner appearance. Preserve the installed component/ID. |
| `MatrixTvTheme.kt` uses a variable Outfit font with `FontVariation.Settings` at weights 300/400/500 | Android variable-font APIs are API 26+. Compose can select older font-loading paths, but identical variable-weight rendering cannot be assumed on 24/25. | Verify the pinned loader behavior and actual text metrics. Ship static weight resources or an explicit older-API fallback if variation settings are ignored; check font footprint, truncation and focus bounds. This is a typography compatibility risk, not a proven Compose crash. |
| `AotOptimizationManager.kt` uses local ADB and `cmd package compile -m speed -f`; broadcast receiver retries tracked-app installs/updates | ART compilation exists on Nougat, but OEM daemon exposure, authorization, package command output and dex status differ. | Test connection, authorization failure, compile/status and package broadcasts on representative API-24/25 firmware. Treat this feature as optional and retain errors/status; never promise compilation success. |
| Home helpers, permissions, installer and storage | Lower OS/firmware changes Home-picker availability, provider support, speech services, storage access and package-install behavior. | Test actual older TVs, not only API-level emulators. Keep current approvals and selected-helper safety policies. |

API 26 is the native introduction point for the time/NIO APIs above. Android documents core-library desugaring for subsets of `java.time` and, with suitable newer tooling/libraries, `java.nio.file`; exact coverage must match the calls used here. [Java API desugaring](https://developer.android.com/studio/write/java8-support). Android documents that preview recommendation channels are available only from API 26. [Channels on the Home screen](https://developer.android.com/training/tv/discovery/recommendations-channel). Variable-font builder APIs are documented at API 26. [Typeface.Builder](https://developer.android.com/reference/android/graphics/Typeface.Builder).

## Existing guards and the lower bound

`MatrixHomeControlScreen.kt` already gates the Home `RoleManager` path at 29 and falls back to Home settings. `UpdateCheck.kt` gates long version codes at 28. `ApkInstaller.kt` gates require-user-action at 31. `DevicePictures.kt` branches image permissions at 33/34 with an older storage permission path. `UpdateInstallResultReceiver.kt` branches newer parcelable access at 33. `MatrixButton.kt` gates RuntimeShader at 33 with an older drawing path. Those uses do not themselves force a 26 minimum.

API **24** also matches existing unguarded `registerDefaultNetworkCallback` in `MatrixRuntime.kt`, device-protected storage in `HomeRecovery.java`/`RemoteConfig.java`, and user-unlocked/direct-boot handling in `GuardRevival.java`. Lowering to **23** needs alternatives/guards for those APIs, boot-state preference migration and notification/accessibility lifecycle tests. The current direct-boot recovery behavior must not simply be removed. Below **23**, the inspected UI dependency floors additionally block installation/build compatibility. No claim is made that all remaining API-23 issues have been enumerated without a lower-minSdk lint/build and device run.

## Footprint and validation trade-offs

Desugared time/NIO implementations increase DEX/APK size and possibly startup/memory work; static font fallbacks add resources. R8 can reduce unused code but does not make missing platform APIs appear. Older TVs are often RAM/storage constrained, so compare optimized universal APK bytes, cold start, heap/graphics memory, frame behavior and compilation disk usage before/after. Existing low-RAM effect gates help rendering, not API compatibility.

A future change needs: a dedicated compatibility branch; resolved/merged-manifest validation and NewApi lint at the proposed minimum; focused clock/quota/cache/atomic-write tests; the full JVM suite; a minified optimized build; then API-24/25 TV startup, D-pad/XML/focus, icon/type metrics, wallpaper permissions/import, SIMKL approval/search/lists, AI provider/tool paths, external playback handoff, confirmed update install and interrupted-install recovery, safe mode, Home/boot/helper rollback and AOT tests. Include API 26 regression coverage. Account/remote/device claims require real integrations; passing JVM tests cannot establish them.

No lower-minSdk build, install or device/account test was performed for this report. Source review and cached metadata support the feasibility assessment, not a compatibility certification. No final optimized artifact was requested or produced, and versionCode remains 154.
