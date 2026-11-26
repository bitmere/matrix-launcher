# Architecture

## Boundaries

- `core`: Android-free policy module (`com.matrix.launcher.core`). Order drafts, package-scoped intent validation, card sizing, Nuvio links, SIMKL cache invalidation and rate-limit decisions. No Android resources, Context, UI, network clients or polling. JSON is compile-only; Android supplies it at runtime.
- `app`: TV Material 3 screens, Android providers, image loading and device services. UI feature files share Matrix theme, card metrics, settings rows/forms and loading states. It depends on core; core cannot depend on app.
- `ComposeHomeRepository`: catalogue/feed assembly and guarded intents. `MatrixAccountsRepository`, `MatrixSearchRepository` and `MatrixWallpaperRepository` own account, explicit catalog search and wallpaper operations. A single SIMKL instance/lock is shared with Home/search so tokens, quota state and cache invalidation cannot diverge.
- `ComposeHomeActivity`: lifecycle-bound refresh/observation and external navigation. Home requests from Inputs, remote mappings and accessibility all target Compose.
- Platform services remain small-purpose Java integrations where retaining tested behavior is useful. There is no retained Java Home/grid/settings UI and no RecyclerView/spring-animation dependency. Kotlin UI does not imply that proven platform policies must be mechanically rewritten.

## Runtime constraints

Provider, account/network, image decode and wallpaper disk work runs on Dispatchers.IO. Platform services retain bounded, one-time local remote-configuration reads. Lazy rows/grids have stable keys and per-row focus restoration. Background transitions blend cached proportional textures up to 1280×720 in ARGB_8888 with dithering at draw time. The texture cache is bounded by 16 MiB of actual allocated bytes; each visible backdrop retains at most two endpoints, with an interrupted transition flattened once off the main thread. No gradient shaders or bitmaps are rebuilt on each animation frame. Theme roles remain stable when focus changes. MatrixRowText shares title/supporting-text alignment, rhythm and focus-aware color. Reorder publishes copied lists, keeps focus requesters under stable item IDs, and reads saved channel ordering for both Home and the editor, including immediate reopen before refresh finishes. Shared image loading is bounded (20% heap memory / 64 MiB disk), with a separate bounded app-banner cache. These bounds are not a measurement of total process memory.

The app catalogue is cached until package events invalidate it. TV provider changes are observed only while started, with a debounce. Clock and wallpaper ticks stop offscreen; connectivity is callback-driven. SIMKL uses its existing activity timestamps, daily/cache policy and shared server cooldowns. No background account sync job was added.

## Build constraints

Two modules are intentional: Android-free policies can compile/test independently without multiplying Android resource tasks. Gradle configuration and build caches are enabled; Kotlin/JVM incremental compilation remains on. Preview builds skip R8/resource shrinking. Final releases run tests/lint then shrink once. Toolchain upgrades are pinned separately from feature migration.

## Source layout

Each screen has its own Kotlin file: Home surface/orchestration, app picker, reorder, list, SIMKL, wallpaper, Home control, quick settings, clock and licenses. Shared controls are MatrixCards, MatrixSettingsComponents, MatrixMenu, MatrixPageHeader, MatrixLayout and MatrixTvTheme. Actual navigation history supplies header breadcrumbs through MatrixPageNavigation. MatrixAppearance owns only the process-local two-color palette. MatrixBackdrop renders it into shared static textures for Home and both panel entry points; MatrixTvTheme keeps stable readable foreground/surface roles. MatrixScrollEdges samples those exact parent pixels in short compositing strips, and MatrixScrollViewport keeps focused controls clear of them. Artwork sampling reuses the repository cache; it creates no second image pipeline or preference writes. Feature screens do not own Back footers or panel colors. MatrixFocusRow keeps the non-scrolling outer margin separate from the shared focus inset for both dock and media rows. MatrixLayout owns portrait sizing and measured portrait-grid column fitting; orientation metadata and image aspect ratios are preserved. ArtworkFit separates stable card slots from the actual fitted image surface, so the rim and progress never surround blank letterbox space. Android provider/account/device adapters are independent of composition and reusable from the Java edition through a reviewed port or the core module.

## Optional search

Settings → Integrations groups SIMKL and TMDB. Search availability requires both an explicit enable toggle and valid local setup (connected SIMKL or a nonblank TMDB credential). Neither provider is a launcher requirement. The inline Search screen keeps its query and results together and uses the same MatrixTextEntry as setup forms and MatrixMediaGrid as list pages. Text submission and a separate optional Voice action share the same request/cache path. Voice delegates through Activity Result to the available system recognition activity; there is no in-app recording service. Metadata extends the shared MediaCard with an optional string; existing app-published/list cards retain their previous height when it is empty.

Search performs one submitted request, maps typed IDs to the existing Nuvio route and keeps at most eight query results for ten minutes in memory. SIMKL extended results include available ratings without per-poster lookups. TMDB multi-search filters out people. New provider screens do not add another image loader, auth client, polling service, exported component or playback engine. One composition-scoped cancellable job owns each explicit search and initial result focus, including equal cached results; no request/completion counters or extra row wrapper are needed. See [setup, cache/privacy boundaries and references](SEARCH.md).

## Primary references

- [Android TV Compose components](https://developer.android.com/training/tv/playback/compose)
- [Compose performance practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Lazy list keys and content types](https://developer.android.com/develop/ui/compose/lists)
- [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Kotlin incremental compilation](https://kotlinlang.org/docs/gradle-compilation-and-caches.html)

## UI roles and inspection

MatrixTvTheme defines one cached Outfit type scale. Display roles keep the light Home/idle clocks; title roles use Medium, supporting body uses Normal, and label roles identify controls. Headers expose heading semantics and form fields expose their actual label. Keep per-screen font-size/weight overrides out of ordinary settings controls.

MatrixMenuItem carries stable identity, command role and nullable checked/selected state together. SettingsPage adapts SettingAction into that rendering model; direct menus use the same model. The shared menu uses ID-based lazy keys/focus restoration and role-based content types. Checkbox/radio semantics are independent of keyboard focus. Visible state markers are trailing, so changing state does not shift the title's leading edge. App-picker cards reuse the same selection semantics.

Home control loads RemoteConfig once per local revision on Dispatchers.IO. Local revision triggers remain because preferences are not yet a complete observable UI model. Removing them without replacing their invalidation would risk stale settings. Platform callbacks and manifest-referenced helpers remain even when static text searches find few callers.

For ADB diagnostics, `adb shell dumpsys activity com.matrix.launcher --matrix-ui` prints page token, focused menu index/generic state, loading/catalogue counts, Home connection/recovery and lifecycle state. It omits titles, account identities, media links, credentials and preference contents. It supplements XML and window/service dumps; it does not verify pixel rendering, every card's focus, physical keys or visual clipping. Run after a frame has settled. Use targeted screenshots for those visual checks.

Foreground entry adds one bounded Home-recovery binding check when recovery is opted in. It does not poll and cannot add a deselected helper; an explicit user removal remains authoritative. No service or permission is newly enabled by an ordinary update.

## Navigation lifetime and permission state

The page SaveableStateHolder lives outside the conditional panel. Pushing a child retains its parent's menu/grid focus, scroll and picker draft. Popping a page removes only that page's saved state; explicitly going Home clears the active stack's state. This prevents cancelled picker drafts leaking into a later visit while preserving drafts during App options/reorder visits. Grid restoration uses card/app IDs through matrixRestoreIndex, with first-item fallback after removal.

One root Back callback serves dialog dismissal, the remote and list headers. A list on a later page or nonfirst card resets to page one's first card before the next Back pops it. Go to Home is an explicit exit. Media rows retain a card ID with safe geometric fallback when that historical card is not currently composed after a data change.

Home request intent actions and CATEGORY_HOME are consumed after dispatch. An external Home return preserves the remembered Home card; another Home press while already on Home resets to the top. The distinction uses stopped-since-resume state because Android pauses a resumed activity before onNewIntent. It does not rely on RESUMED inside that callback.

TV-listings permission is activity-owned observable state, refreshed on start and permission result. Home channels shows access status and only offers Allow TV listings while missing; a previously blocked request opens this app's permission settings. Reload remains a regular row with a refresh icon, and repeated requests while loading are ignored. Permission access and data refresh remain separate operations. Right-side panels retain rounded exposed left corners and screen-aligned right edges.

References: [SaveableStateHolder](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/SaveableStateHolder), [Activity.onNewIntent](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)).

### Vendor startup access during migration

The TCL G08 device has a separate vendor AUTO_START app-op. The Java package's allow state does not transfer to the Compose application ID. Boot logs must be checked for vendor denials before calling receiver recovery broken. On this device the Compose receiver and service were denied while AUTO_START was ignore; the approved startup setup changed only that package to allow and persisted app-op settings. Preserve the original mode for undo. Do not assume this operation exists on other TVs or call hidden vendor APIs from the app.

## One control appearance (beta 25)

MatrixControlStyle is the single base shape/fill/stroke/padding definition. MatrixButton applies it to every nondestructive role: settings options, immediate commands, commits, secondary navigation, header Back and Home toolbar icons. Role metadata determines behavior, commit icon, compact placement or destructive color; it does not fork neutral controls into gray rounded rectangles versus outlined pills. Menu rows share one width/text layout, with only commit actions compact and end-aligned. Form fields reuse the same geometry/tone, keeping their editing focus outline; neutral loading placeholders reuse the shape. Media poster/app artwork and edge-attached panels retain their content-specific geometry.

The structural audit rejects raw TV Button/ButtonDefaults imports in feature screens. This boundary prevents another manual toolbar or feature-button style from bypassing MatrixButton. It is not a pixel screenshot test or a ban on necessary media/rendering colors.

## Icon-only geometry (beta 26)

MatrixIconButton uses TV Material IconButton's centered Box layout, while MatrixButton is for text/row content. Both consume the same MatrixControlStyle and shared color/border builders. Sharing visual tokens does not mean forcing different content types through text-button sizing/padding. Removed the Back role's text-button padding workaround. Header controls use a 48 dp frame, toolbar controls 54 dp and regular glyphs 24 dp, defined once; inline commit marks retain their own 20 dp type-aligned role. Media artwork remains aspect-driven content.

The existing logical dump now includes IconLayout frame/glyph dimensions and center deltas. These are process-local coordinates, not screenshots, and include only application-defined quick-control labels. Read-only `tools/check_icon_layout.py --adb <adb> --serial <device> --min-icons 5` asserts settled frame centering within half a physical pixel. Screenshots/pixel bounds are still required to verify the painted vector artwork; allocation bounds alone do not prove optical alignment. Geometry entries are removed when a control leaves composition. No preference, polling loop or per-frame layout state is written.

Reference: [TV Material IconButton implementation](https://android.googlesource.com/platform/frameworks/support/+/2eca65cef01108bfee1e9f29645181eb2c1859d9/tv/tv-material/src/main/java/androidx/tv/material3/IconButton.kt).

## Wallpaper source boundary

MatrixWallpaperRepository serializes import, selection, credit reads and Commons refresh on Dispatchers.IO. PersonalWallpapers owns local storage/rotation and an idempotent Reddit preference retirement; no files are moved or removed by that migration. CommonsWallpapers owns public metadata parsing, a strict HTTPS host/license/dimension filter, sequential bounded downloads and paired attribution sidecars. It reuses PersonalImages and the existing JPEG/downsample path; no worker service, OAuth client or image-cache dependency is added. Daily downloads are opt-in for shared installs and run only while the activity is started; manual refresh has a one-minute cooldown. Both modes retain previous images on network failure.

## Whole-project audit corrections

The unused PersonalImages instance cache/loader is removed: Coil is the active UI image pipeline, while static bounded image/network utilities remain shared by providers. Client ID mutation and cache deletion share the account IO mutex. App-banner decoding coalesces misses; generation-based invalidation uses a short cache lock and does not wait for decode. Wallpaper store creation and SIMKL cache-directory creation are deferred to IO work. Playback progress has one TV-theme rounded renderer with standard progress semantics; cards remain TV Material ClassicCard and the single artwork rim is retained. Dynamic menu labels have stable IDs. See PROJECT-REVIEW.md for coverage, removed legacy compatibility routes and the historical cache finding, now addressed by SimklDiskCache.

## Component audit follow-up (beta 53)

App-catalogue cache entries are generation-tagged across package invalidation; cancelled refreshes cannot clear a newer loading state. Remote launch paths reuse RemoteKeys.start, and custom SIMKL preview/full pages share bounded valid-link mapping. Vendor input errors produce a recoverable menu state; empty credits use the shared status page. No provider, Home-capture mode or image library was replaced. See [historical component audit](CODE-REVIEW-53.md).

## Remaining-work follow-up (beta 55)

RemoteConfig.Loader moves overlay reads off main with component-owned generation cancellation; application/service mutations remain on main. SimklDiskCache serializes bounded atomic cache replacements and retains five metadata/library snapshots plus up to four pinned first pages. Home retains one viewport behind and half ahead; default nested prefetch is preserved after a visible-count experiment regressed. Row derivations and 128 artwork aspect floats are reused without increasing bitmap-cache limits. [Validation and tradeoffs](OPTIMIZATION-54.md).

## Shared Nuvio handoff (beta 56)

Home channels, SIMKL and Search reuse ComposeHomeRepository.mediaIntent. Detail requests resolve Nuvio's exported public handler and use a fresh content-only handoff; native Continue Watching requests retain stream/episode/resume context. App icons remain ordinary resume launches. Typed parsing now lives in Android-free MediaLinks (named NuvioLinks in stable 1.1). No task reset, force-stop or duplicate per-screen adapter. [Version-tested contract](NUVIO-ROUTING.md).

## Configurable media target (local 1.1.1-beta.3)

Catalog providers now produce internal MediaLinks identities and matrix.catalog source markers. MatrixMediaRouter owns installed Nuvio/Stremio detection, persisted selection, fresh detail construction and native publisher intent handling; ComposeHomeRepository delegates once. SIMKL cards retain their source ID/endpoint for one on-click detail lookup through the existing SimklClient auth/quota/cache path. TMDB-only cards reuse MatrixSearchRepository's authenticated HTTP path for IMDb mapping. Shared lifecycle-owned launch cancellation prevents a late lookup from opening an app after navigation/stop. No new dependency or permission. [Setup and future adapter contract](MEDIA-APPS.md).

Stremio detail delivery uses NEW_TASK/CLEAR_TOP, while Nuvio uses NEW_TASK/CLEAR_TOP/SINGLE_TOP. These are distinct tested Activity contracts, not one universal flag preset. Native app channels keep their published routing flags.

## Matrix namespace and stable Android identities

Application implementation/resources and JVM app tests use com.matrix.launcher, with reusable core in com.matrix.launcher.core. Four minimal legacy service subclasses and one Home activity-alias preserve Android approvals, saved defaults and shortcuts while logic moves to Matrix. New actions/tools use Matrix names; configured old remote actions still parse. The installed application ID, provider authority and private preference names stay stable. [Migration contract and runtime validation](NAMESPACE.md).

## Wallpaper import and selection (beta 6)

MatrixWallpaperScreen uses GET_CONTENT for ephemeral chosen-image access and immediately copies into the existing store. Saved pictures reuse MatrixMediaGrid, shared card geometry and repository selection. WallpaperImportActivity accepts only validated VIEW/SEND image content URIs, previews before confirmation, never forwards incoming Intents and adds no broad storage permission. URI validation is Android-free SharedImagePolicy. One process-wide repository mutex serializes main/import Activity storage; imports retain the selected local image while enforcing the existing limit. [Setup, security bounds and validation](WALLPAPERS.md).

### Remote-friendly device wallpaper browser (local beta 7)

DevicePictures is a read-only MediaStore adapter with contextual image-only access and a bounded IO query. MatrixDevicePictures handles permission-result/resume reloads and composes existing SettingsPage album rows and MatrixMediaGrid cards. Selected URIs use MatrixWallpaperRepository's shared mutex and bounded JPEG pipeline. No duplicate controls/cache, background indexing observer, additional Activity, new dependency or all-files permission. Shared-image imports remain permission-independent.

## Home surfaces and focus (2.0)

Shared controls use translucent surfaces and cached focus decoration. Focus rotation is draw-only and lifecycle-bound; Android 13+ uses a bounded AGSL outward glow with an older-device stroke fallback. Existing artwork palettes supply colour without another decoder. Card geometry reserves the glow footprint. Continue Watching draws a 3dp translucent track and light elapsed fill without a backing/outline, retaining standard progress semantics and the card's watched-percentage state. Settings descriptions wrap; stable menu/form identity mappings are remembered.
