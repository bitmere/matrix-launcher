# Data and core review

Reviewed every file listed below for callers, data flow, I/O placement, cache/auth behavior and duplication. This is a source review, not proof of real-account completion or every provider/device combination.

## Production coverage

App: `ComposeHomeRepository.kt`, `MatrixAccountsRepository.kt`, `MatrixWallpaperRepository.kt`, `CommonsWallpapers.kt`, `PersonalImages.java`, `PersonalWallpapers.java`, `SimklClient.java`, `TvFeed.java`, `HomeArtwork.java`, `HomeFeedSource.java`, `HomeRows.java`, `HomeSections.java`, `ArtworkColor.java`.

Core: `FeedRules.java`, `HomeCardSizing.java`, `NuvioLinks.java`, `PackageNames.java`, `ReorderSession.java`, `SimklCachePolicy.java`, `SimklRequestPolicy.java`.

## Test coverage

App: `CommonsWallpapersTest.kt`, `WallpaperMigrationTest.kt`, `SimklCacheFallbackTest.java`, `SimklMappingTest.java`, `HomeRowsTest.java`, `HomeSectionsTest.java`.

Core: `FeedRulesTest.java`, `HomeCardSizingTest.java`, `NuvioLinksTest.java`, `PackageNamesTest.java`, `ReorderSessionTest.java`, `SimklCachePolicyTest.java`, `SimklRequestPolicyTest.java`.

Existing tests exercise separate license/host/size, upgrade, authentication/fallback, identity/link, preview/pagination, source ordering, request cooldown and cache-invalidation regressions. No test depended on the unused PersonalImages instance loader. Subtitle-only assertions were removed while preserving tests for title/image/link and pagination behavior. A provider portrait-contract regression was added. A HomeRecovery assertion is misplaced in HomeRowsTest but not duplicated; it was retained to avoid unrelated test churn.

## Fixed findings

- Removed the unused PersonalImages instance image loader, memory/disk poster caches, downloader and trim code after confirming there were no production constructors/callers. Compose uses Coil. Active static bounded decode/transfer helpers remain.
- Removed unread SIMKL fallback ThreadLocal tracking, cacheNotice and beginRead calls. Actual cache fallback and server cooldown behavior remain.
- Client ID mutation now runs on IO under the same mutex as account/Home operations; previously it could delete cache files on the UI thread and race an active request. The account screen awaits this operation.
- App-banner cache misses are coalesced. Invalidation uses a short cache monitor and generation check, so the main thread does not wait for binder/decode work and a stale decode cannot repopulate an invalidated cache.
- Wallpaper storage initializes lazily within IO/mutex calls. SIMKL creates its cache directory on cache writes rather than in its constructor.
- Wallpaper imports check coroutine activity between images; a Java thread interruption check did not reflect ordinary coroutine cancellation.
- TV provider poster aspect metadata is read. Official portrait constants 4/5/6 are recognized; absent and unknown values keep the landscape default. See the [Android TV provider contract](https://developer.android.com/reference/kotlin/androidx/tvprovider/media/tv/TvContractCompat.PreviewProgramColumns).
- Removed unused Compose and Java subtitle fields and SIMKL year/rating/next-episode string formatting. They had no production reader. Title, artwork, target link and playback progress mapping remain.

## Deferred and verification limits

SIMKL pagination responses are bounded per entry but there is no overall disk-cache cap. Adding global eviction requires a deliberate policy for offline library/list retention and should be validated with multiple accounts/lists; it was not hidden inside this cleanup.

Real account approval, source-app profile changes, actual provider cursor metadata and cancelling an in-progress document import need integration/device checks. Unit tests alone do not establish those. No API migration, app-account extraction, background polling service or new dependency was introduced by this backend pass.
