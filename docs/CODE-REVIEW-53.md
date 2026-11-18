# Component audit — beta 53, 2026-11-04

Scope: 77 production Kotlin/Java files (6,019 lines), source resources/manifest, Gradle/R8 boundaries, targeted JVM fixtures and device-test tools. This component/caller review and automated reference scan is not a proof that every possible firmware, account or event interleaving is defect-free. Generated outputs, vendored binaries, image assets and ignored device artifacts are not treated as handwritten code to delete.

## Implemented corrections

- Activity refresh: cancellation cannot clear a newer refresh's loading state. App catalogue entries carry their invalidation generation, so a late PackageManager scan cannot seed a stale reusable catalogue after a package broadcast.
- Remote routes: four duplicated launch/catch branches and the Input helper's duplicate input launch use the existing shared launcher. Actions, flags and failure behavior are retained.
- SIMKL: search/library parsing skips non-object records; unusable IDs no longer consume library pagination slots. Home custom previews and full custom pages share one bounded, valid-link mapper.
- UI resilience: Inputs catches a failed vendor service read instead of failing the composition coroutine. Licenses wait for a frame before requesting focus. Empty/loading wallpaper credits are proper status screens, not selectable no-op actions.
- Tooling: XML smoke checks follow Integrations and handle connected accounts. Scrolling stress defaults to explicit Matrix intents; optional --home-key-reset tests synthetic routing separately. Frame-measurement tooling verifies foreground before initial D-pad input and each measured key. Removed an obsolete Classic-settings comment.

## Retained intentionally

Manifest entry points, network/input-device callbacks, cache eviction overrides, boot/standby helpers and protected config.json access have real platform/configuration callers. No unreferenced source resource candidate was found. Do not delete these from a one-reference text count. Shared TV Material 3 cards/controls, custom TV-theme progress, source-fitted artwork, gradients, bounded image caches, optional providers and the existing request cooldown remain. No new dependency or permission.

## Remaining optimization work / limits

Historical findings below describe beta 53. The startup overlay and global cache budget are addressed in the [beta 55 follow-up](OPTIMIZATION-54.md), included in stable 1.1.0; residual motion spikes are measured there.

- SIMKL files are individually bounded, but disk cache has no global retention cap. Designing eviction must preserve deliberate offline library/index/pinned-list behavior; no cached user collection was deleted in this pass.
- RemoteConfig.load still performs a bounded (256 KiB maximum) synchronous overlay read from Activity/service startup paths; settings/package-manager Binder calls also remain there. File/network/cache work in data repositories is dispatched to IO, backdrop/QR computation to Default, and ServiceRepair's sleeps run on its own worker. The synchronous startup overlay is a remaining main-thread stall risk, not a measured ANR finding or a claim of zero ANR risk. A lifecycle-safe asynchronous service-config change needs its own capture/boot regression pass.
- Existing orientation lint warning is retained for this TV-only app. Debug signing, target SDK 34 and the pinned dependency versions are unchanged; a signing/SDK migration is outside this cleanup.
- No new runtime speed or lower RAM claim is inferred from line count, unit tests or a single graphics dump. Physical Home/volume and provider-specific playback remain separate from synthetic D-pad navigation.

## Validation

Installed, exact APK hash verified. All 126 JVM tests and two parser tests pass; no new lint warning. Forty route checkpoints, two explicit-intent scrolling stress passes and Search regression checks pass. Settled rows/icons and final Search capture were checked. Synthetic HOME routing failed and stopped input; the user subsequently confirmed real Home consistently opens Matrix. Stress frames still show ~21–22% jank. See [validation](VALIDATION.md) for exact outcomes, limits and artifact hash. Tests cover 10,000 deterministic geometry/reveal cases and malformed SIMKL search/custom-list/library records.

## Production file ledger

Each file was examined in its owning component for callers, lifecycle/state, I/O, failure handling, duplication and keep/delete decisions. The table records scope; it is not an automated correctness certificate.

| Source | Area | Lines |
| --- | --- | --- |
| `app/src/main/java/com/matrix/launcher/AppLaunchActivity.java` | Platform/remote/security | 32 |
| `app/src/main/java/com/matrix/launcher/AppManagement.java` | Platform/remote/security | 31 |
| `app/src/main/java/com/matrix/launcher/ArtworkColor.java` | Data/cache | 48 |
| `app/src/main/java/com/matrix/launcher/ArtworkFit.kt` | UI/layout | 13 |
| `app/src/main/java/com/matrix/launcher/ArtworkPalette.kt` | UI/layout | 12 |
| `app/src/main/java/com/matrix/launcher/BackdropQuality.kt` | UI/layout | 15 |
| `app/src/main/java/com/matrix/launcher/ClockActivity.kt` | UI/layout | 33 |
| `app/src/main/java/com/matrix/launcher/ClockDream.kt` | UI/layout | 40 |
| `app/src/main/java/com/matrix/launcher/CommonsWallpapers.kt` | Data/cache | 123 |
| `app/src/main/java/com/matrix/launcher/ComposeHomeActivity.kt` | Platform/remote/security | 187 |
| `app/src/main/java/com/matrix/launcher/ComposeHomeModels.kt` | UI/layout | 13 |
| `app/src/main/java/com/matrix/launcher/ComposeHomeRepository.kt` | Data/cache | 129 |
| `app/src/main/java/com/matrix/launcher/ComposeRevealPolicy.kt` | UI/layout | 19 |
| `app/src/main/java/com/matrix/launcher/GamepadPassthrough.java` | Platform/remote/security | 85 |
| `app/src/main/java/com/matrix/launcher/GuardConfig.java` | Platform/remote/security | 20 |
| `app/src/main/java/com/matrix/launcher/GuardKeep.java` | Platform/remote/security | 111 |
| `app/src/main/java/com/matrix/launcher/GuardRevival.java` | Platform/remote/security | 56 |
| `app/src/main/java/com/matrix/launcher/HomeArtwork.java` | Data/cache | 74 |
| `app/src/main/java/com/matrix/launcher/HomeFeedSource.java` | Data/cache | 28 |
| `app/src/main/java/com/matrix/launcher/HomeKeyService.java` | Platform/remote/security | 482 |
| `app/src/main/java/com/matrix/launcher/HomeRecovery.java` | Platform/remote/security | 82 |
| `app/src/main/java/com/matrix/launcher/HomeRequest.java` | Platform/remote/security | 27 |
| `app/src/main/java/com/matrix/launcher/HomeRows.java` | Data/cache | 40 |
| `app/src/main/java/com/matrix/launcher/HomeSections.java` | Data/cache | 26 |
| `app/src/main/java/com/matrix/launcher/InputKeyService.java` | Platform/remote/security | 97 |
| `app/src/main/java/com/matrix/launcher/InputsRequestActivity.java` | Platform/remote/security | 20 |
| `app/src/main/java/com/matrix/launcher/MatrixAccountNavigation.kt` | UI/layout | 5 |
| `app/src/main/java/com/matrix/launcher/MatrixAccountScreen.kt` | UI/layout | 157 |
| `app/src/main/java/com/matrix/launcher/MatrixAccountsRepository.kt` | Data/cache | 28 |
| `app/src/main/java/com/matrix/launcher/MatrixAppearance.kt` | UI/layout | 37 |
| `app/src/main/java/com/matrix/launcher/MatrixAppGridScreen.kt` | UI/layout | 66 |
| `app/src/main/java/com/matrix/launcher/MatrixBackdrop.kt` | UI/layout | 121 |
| `app/src/main/java/com/matrix/launcher/MatrixButton.kt` | UI/layout | 118 |
| `app/src/main/java/com/matrix/launcher/MatrixCards.kt` | UI/layout | 229 |
| `app/src/main/java/com/matrix/launcher/MatrixClockScreen.kt` | UI/layout | 47 |
| `app/src/main/java/com/matrix/launcher/MatrixComposeApplication.kt` | UI/layout | 15 |
| `app/src/main/java/com/matrix/launcher/MatrixFocusRow.kt` | UI/layout | 50 |
| `app/src/main/java/com/matrix/launcher/MatrixHomeControlScreen.kt` | UI/layout | 66 |
| `app/src/main/java/com/matrix/launcher/MatrixHomeScreen.kt` | UI/layout | 198 |
| `app/src/main/java/com/matrix/launcher/MatrixHomeSurface.kt` | UI/layout | 200 |
| `app/src/main/java/com/matrix/launcher/MatrixLayout.kt` | UI/layout | 51 |
| `app/src/main/java/com/matrix/launcher/MatrixLicenseScreen.kt` | UI/layout | 50 |
| `app/src/main/java/com/matrix/launcher/MatrixListScreen.kt` | UI/layout | 43 |
| `app/src/main/java/com/matrix/launcher/MatrixMediaGrid.kt` | UI/layout | 45 |
| `app/src/main/java/com/matrix/launcher/MatrixMenu.kt` | UI/layout | 103 |
| `app/src/main/java/com/matrix/launcher/MatrixOrderScreen.kt` | UI/layout | 121 |
| `app/src/main/java/com/matrix/launcher/MatrixPageHeader.kt` | UI/layout | 41 |
| `app/src/main/java/com/matrix/launcher/MatrixPageNavigation.kt` | UI/layout | 43 |
| `app/src/main/java/com/matrix/launcher/MatrixRuntime.kt` | UI/layout | 32 |
| `app/src/main/java/com/matrix/launcher/MatrixScrollEdges.kt` | UI/layout | 72 |
| `app/src/main/java/com/matrix/launcher/MatrixSearchRepository.kt` | Data/cache | 94 |
| `app/src/main/java/com/matrix/launcher/MatrixSearchScreen.kt` | UI/layout | 171 |
| `app/src/main/java/com/matrix/launcher/MatrixSettingsComponents.kt` | UI/layout | 137 |
| `app/src/main/java/com/matrix/launcher/MatrixTvTheme.kt` | UI/layout | 72 |
| `app/src/main/java/com/matrix/launcher/MatrixUiInspection.kt` | UI/layout | 51 |
| `app/src/main/java/com/matrix/launcher/MatrixWallpaperRepository.kt` | Data/cache | 36 |
| `app/src/main/java/com/matrix/launcher/MatrixWallpaperScreen.kt` | UI/layout | 64 |
| `app/src/main/java/com/matrix/launcher/PersonalImages.java` | Data/cache | 55 |
| `app/src/main/java/com/matrix/launcher/PersonalWallpapers.java` | Data/cache | 104 |
| `app/src/main/java/com/matrix/launcher/ProbeActivity.java` | Platform/remote/security | 23 |
| `app/src/main/java/com/matrix/launcher/QuickSettingsActivity.kt` | Platform/remote/security | 43 |
| `app/src/main/java/com/matrix/launcher/RemoteConfig.java` | Platform/remote/security | 76 |
| `app/src/main/java/com/matrix/launcher/RemoteKeys.java` | Platform/remote/security | 431 |
| `app/src/main/java/com/matrix/launcher/ServiceRepair.java` | Platform/remote/security | 107 |
| `app/src/main/java/com/matrix/launcher/SimklClient.java` | Data/cache | 289 |
| `app/src/main/java/com/matrix/launcher/SystemSettings.java` | Platform/remote/security | 63 |
| `app/src/main/java/com/matrix/launcher/ThemeFiles.java` | Platform/remote/security | 13 |
| `app/src/main/java/com/matrix/launcher/ThemeProvider.java` | Platform/remote/security | 51 |
| `app/src/main/java/com/matrix/launcher/TvFeed.java` | Data/cache | 150 |
| `app/src/main/java/com/matrix/launcher/VolumeKeys.java` | Platform/remote/security | 67 |
| `core/src/main/java/com/matrix/launcher/core/FeedRules.java` | Core policy | 27 |
| `core/src/main/java/com/matrix/launcher/core/HomeCardSizing.java` | Core policy | 13 |
| `core/src/main/java/com/matrix/launcher/core/NuvioLinks.java` | Core policy | 8 |
| `core/src/main/java/com/matrix/launcher/core/PackageNames.java` | Core policy | 21 |
| `core/src/main/java/com/matrix/launcher/core/ReorderSession.java` | Core policy | 16 |
| `core/src/main/java/com/matrix/launcher/core/SimklCachePolicy.java` | Core policy | 21 |
| `core/src/main/java/com/matrix/launcher/core/SimklRequestPolicy.java` | Core policy | 35 |
