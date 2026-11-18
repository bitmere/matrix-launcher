# Project review - beta 34

Historical review. See the [1.1 release summary](RELEASE-1.1.md) for current status, the [beta 53 audit](CODE-REVIEW-53.md) for component coverage and the [optimization follow-up](OPTIMIZATION-54.md) for resolved startup/cache findings.

Reviewed by three area-specific subagents plus the parent review of build/test/device tools. Existing Apple-style Home and the corrected single card rim are retained. This is a source/caller review, tests and targeted integration validation, not proof that every firmware/account/provider combination is bug-free.

## Implemented findings

- Removed unused PersonalImages instance poster caches/loader; Coil remains the active image pipeline and static bounded transfer/decode helpers remain.
- Removed unread SIMKL fallback tracking and unused subtitle/rating/year/next-episode formatting across models/mappers; kept real title/image/link/progress data.
- Removed unused OpenOnStart implementation/tests, unused remote menu-release policy/test and uncalled power-menu helpers. Preserved the one legacy preference key required by Home capture.
- Removed nonfunctional legacy wallpaper/caption/revision provider routes. The protected config.json endpoint remains; active local/Commons import is separate.
- SIMKL Client ID writes/cache invalidation now run on IO under the account mutex. Banner decode misses coalesce; invalidation never waits for decode and cannot repopulate stale entries.
- Deferred wallpaper directory lookup and SIMKL cache-directory creation to IO. Imported images honor coroutine cancellation between files.
- Provider portrait aspect constants are now read. Stabilized setting identity across changing labels, reused date formatters, removed duplicate source filtering/imports.
- Corrected vendor settings fallback to check the exact enabled/resolvable component.
- Fixed mixed app/core filtered test execution and stale Reddit smoke steps.
- Device inspection caught a stale focus-group read suppressing the first Down reveal from Favorites. The scrolling callback now reads the current focus state directly.

## Material 3 component coverage

| Area | Result |
| --- | --- |
| App/media/action cards | Already TV Material 3 ClassicCard; retain fitted artwork and single shared border. |
| Settings, commands and header/toolbar buttons | Existing TV Material Button/IconButton/Surface with shared Matrix roles retained. |
| Typography/colors | Shared TV Material theme and Outfit roles retained; dynamic setting identities are separate from labels. |
| Playback progress | One rounded shared renderer now uses TV theme roles and standard progress semantics. TV Material has no progress indicator; do not import the mobile theme/component stack for one non-interactive bar. |
| Text entry, scroll fades, backdrops, skeletons | Foundation/custom drawing retained where TV Material supplies no equivalent. Platform input/D-pad behavior and app visual direction remain intentional. |

Android recommends the TV-specific component library: [Compose on TV](https://developer.android.com/training/tv/playback/compose). See [progress guidance](https://developer.android.com/develop/ui/compose/components/progress) for semantics and determinate indicators. No new runtime dependency was added.

## Remaining limits

SIMKL pagination responses are bounded individually but the disk cache lacks a global cap. A future eviction policy must define offline retention and preserve auth/library metadata deliberately. Fresh SIMKL authorization, real provider-cursor variants, import cancellation during an actual transfer and vendor-specific fallbacks require integration tests beyond JVM policy tests. No unreferenced UI classes/resources were proven: manifest callbacks, optional integrations and XML references remain. Small test assertions are not duplicated behavior worth deleting merely to lower the test count.

## File coverage

Every current production source and JVM test below was reviewed in its assigned area. Removed files were reviewed before deletion and are documented in the findings. Area reports: [data](AUDIT-DATA.md), [platform](AUDIT-PLATFORM.md). The UI reviewer fully read 35 UI/geometry/runtime files and six UI policy test files; shared runtime files also received platform review.

| File | Reviewer area |
| --- | --- |
| `app/src/main/java/com/matrix/launcher/AppLaunchActivity.java` | Platform |
| `app/src/main/java/com/matrix/launcher/AppManagement.java` | Platform |
| `app/src/main/java/com/matrix/launcher/ArtworkColor.java` | Data |
| `app/src/main/java/com/matrix/launcher/ArtworkFit.kt` | UI |
| `app/src/main/java/com/matrix/launcher/ArtworkPalette.kt` | UI |
| `app/src/main/java/com/matrix/launcher/BackdropQuality.kt` | UI |
| `app/src/main/java/com/matrix/launcher/ClockActivity.kt` | UI |
| `app/src/main/java/com/matrix/launcher/ClockDream.kt` | UI |
| `app/src/main/java/com/matrix/launcher/CommonsWallpapers.kt` | Data |
| `app/src/main/java/com/matrix/launcher/ComposeHomeActivity.kt` | Platform |
| `app/src/main/java/com/matrix/launcher/ComposeHomeModels.kt` | UI |
| `app/src/main/java/com/matrix/launcher/ComposeHomeRepository.kt` | Data |
| `app/src/main/java/com/matrix/launcher/ComposeRevealPolicy.kt` | UI |
| `app/src/main/java/com/matrix/launcher/GamepadPassthrough.java` | Platform |
| `app/src/main/java/com/matrix/launcher/GuardConfig.java` | Platform |
| `app/src/main/java/com/matrix/launcher/GuardKeep.java` | Platform |
| `app/src/main/java/com/matrix/launcher/GuardRevival.java` | Platform |
| `app/src/main/java/com/matrix/launcher/HomeArtwork.java` | Data |
| `app/src/main/java/com/matrix/launcher/HomeFeedSource.java` | Data |
| `app/src/main/java/com/matrix/launcher/HomeKeyService.java` | Platform |
| `app/src/main/java/com/matrix/launcher/HomeRecovery.java` | Platform |
| `app/src/main/java/com/matrix/launcher/HomeRequest.java` | Platform |
| `app/src/main/java/com/matrix/launcher/HomeRows.java` | Data |
| `app/src/main/java/com/matrix/launcher/HomeSections.java` | Data |
| `app/src/main/java/com/matrix/launcher/InputKeyService.java` | Platform |
| `app/src/main/java/com/matrix/launcher/InputsRequestActivity.java` | Platform |
| `app/src/main/java/com/matrix/launcher/MatrixAccountNavigation.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixAccountScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixAccountsRepository.kt` | Data |
| `app/src/main/java/com/matrix/launcher/MatrixAppearance.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixAppGridScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixBackdrop.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixButton.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixCards.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixClockScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixComposeApplication.kt` | Platform |
| `app/src/main/java/com/matrix/launcher/MatrixFocusRow.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixHomeControlScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixHomeScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixHomeSurface.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixLayout.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixLicenseScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixListScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixMenu.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixOrderScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixPageHeader.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixPageNavigation.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixRuntime.kt` | Platform |
| `app/src/main/java/com/matrix/launcher/MatrixScrollEdges.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixSettingsComponents.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixTvTheme.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixUiInspection.kt` | UI |
| `app/src/main/java/com/matrix/launcher/MatrixWallpaperRepository.kt` | Data |
| `app/src/main/java/com/matrix/launcher/MatrixWallpaperScreen.kt` | UI |
| `app/src/main/java/com/matrix/launcher/PersonalImages.java` | Data |
| `app/src/main/java/com/matrix/launcher/PersonalWallpapers.java` | Data |
| `app/src/main/java/com/matrix/launcher/ProbeActivity.java` | Platform |
| `app/src/main/java/com/matrix/launcher/QuickSettingsActivity.kt` | UI |
| `app/src/main/java/com/matrix/launcher/RemoteConfig.java` | Platform |
| `app/src/main/java/com/matrix/launcher/RemoteKeys.java` | Platform |
| `app/src/main/java/com/matrix/launcher/ServiceRepair.java` | Platform |
| `app/src/main/java/com/matrix/launcher/SimklClient.java` | Data |
| `app/src/main/java/com/matrix/launcher/SystemSettings.java` | Platform |
| `app/src/main/java/com/matrix/launcher/ThemeFiles.java` | Platform |
| `app/src/main/java/com/matrix/launcher/ThemeProvider.java` | Platform |
| `app/src/main/java/com/matrix/launcher/TvFeed.java` | Data |
| `app/src/main/java/com/matrix/launcher/VolumeKeys.java` | Platform |
| `app/src/test/java/com/matrix/launcher/AppManagementTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/ArtworkFitTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/BackdropQualityTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/CommonsWallpapersTest.kt` | Data |
| `app/src/test/java/com/matrix/launcher/ComposeRevealPolicyTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/GamepadPassthroughTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/HomeKeyServiceTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/HomeRequestTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/HomeRowsTest.java` | Data |
| `app/src/test/java/com/matrix/launcher/HomeSectionsTest.java` | Data |
| `app/src/test/java/com/matrix/launcher/MatrixAccountNavigationTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/MatrixLayoutTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/MatrixPageNavigationTest.kt` | UI |
| `app/src/test/java/com/matrix/launcher/RemoteConfigTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/RemoteKeysTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/SimklCacheFallbackTest.java` | Data |
| `app/src/test/java/com/matrix/launcher/SimklMappingTest.java` | Data |
| `app/src/test/java/com/matrix/launcher/ThemeFilesTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/VolumeKeysTest.java` | Platform |
| `app/src/test/java/com/matrix/launcher/WallpaperMigrationTest.kt` | Data |
| `core/src/main/java/com/matrix/launcher/core/FeedRules.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/HomeCardSizing.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/MediaLinks.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/PackageNames.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/ReorderSession.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/SimklCachePolicy.java` | Data/core |
| `core/src/main/java/com/matrix/launcher/core/SimklRequestPolicy.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/FeedRulesTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/HomeCardSizingTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/MediaLinksTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/PackageNamesTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/ReorderSessionTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/SimklCachePolicyTest.java` | Data/core |
| `core/src/test/java/com/matrix/launcher/core/SimklRequestPolicyTest.java` | Data/core |

Manifest, all resources/XML/configuration, app Gradle and ProGuard rules received platform review; packaged fonts/licenses/icon assets were retained as referenced licensed assets. Root Gradle settings, core/build configuration, build/install/text-entry/ART scripts and all five Python tools received parent review. Historical validation documents remain historical rather than being rewritten as current proof.

Production diff at review: 111 lines added, 263 removed (net 152 removed). Counts describe source changes, not measured runtime speed.

## 1.3 focused follow-up

See [release scope](RELEASE-1.3.md) for the UI/backend findings and limits. The historical inventory above describes its original audit, not a new exhaustive certification.
