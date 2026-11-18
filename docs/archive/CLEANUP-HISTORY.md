# Independent Compose cleanup

Initial independent-repository migration, compared with Compose prototype `980a6e4` (these historical counts are not the current beta source totals):

- 74 obsolete/replaced production classes removed.
- Production source lines: 18521 â†’ 4694 (app + core).
- Removed RecyclerView and DynamicAnimation dependencies.
- Removed legacy XML layouts, unused resource entries, stale translations and Classic configuration/effect engines. Current UI is English.
- Relevant pure policies moved to the JVM-only core module. Backend/device integration remains only where used by Compose.

## Retired classes

AmbientBackdrop, AppAdapter, AppLibraryDialog, BatFlightView, BellDrawable, CardFeature, CardPageSurface, Cards, ClockFormat, ClockScene, Config, CoocaaSources, DiyaDrawable, DockDecoration, DockSlots, Festival, FestiveLamp, GameDay, GlassFocus, GlassView, GridNavigation, HeroArt, HomeAlignment, HomeCard, HomeDataLoader, HomeDefaults, HomeDialog, HomeInputs, HomeMenus, HomeNetworkStatus, HomeOrderEditor, HomePanelRows, HomeScroll, HomeSettingsActivity, HomeSidePanel, HomeStyle, HomeToolbar, InputFold, InputNames, InputSources, JackOLanternDrawable, LastOpened, LaunchActions, LauncherLocale, LauncherSettingsActivity, LibraryApps, LibraryLoads, LoadingSkeleton, Lunar, MainActivity, MarigoldDrawable, MemoryTrimPolicy, Motion, Order, PersonalHomeActivity, PetalShowerView, PlaybackArtwork, RaceWeekend, RowFocusMemory, Shamsi, Sky, SkyView, SpookyIconDrawable, StatsText, Sun, TileRefresh, TopFade, UiFont, VpnBadge, WallpaperCaptions, WallpaperMood, WallpaperPlayer, WallpaperRotation, WallpaperSize

## Retired tests

AppearanceConfigTest.java, BannerShapeTest.java, CardFeatureTest.java, CardsTest.java, ClockFormatTest.java, DockCornersTest.java, DockSlotsTest.java, FestivalTest.java, GameDayTest.java, GridNavigationTest.java, HomeDefaultsTest.java, HomeScrollTest.java, InputFoldTest.java, InputNamesTest.java, InputSourcesTest.java, LaunchActionsTest.java, LauncherLocaleTest.java, LibraryAppsTest.java, LibraryLoadsTest.java, LunarTest.java, MemoryTrimPolicyTest.java, MotionTest.java, OrderTest.java, PackageMapTest.java, RaceWeekendTest.java, RowFocusMemoryTest.java, ShamsiTest.java, SkyTest.java, SpringFrames.java, StatsTextTest.java, SunTest.java, TileFitTest.java, TileRefreshTest.java, TopFadeTest.java, VpnPillTest.java, WallpaperCaptionsTest.java, WallpaperMoodTest.java, WallpaperRotationTest.java, WallpaperSizeTest.java

These tests exercised implementations absent from this app. Relevant source/auth/cache/quota, reorder, intent and remote tests remain; new overlay/package validation checks were added. Removed behavior and history remain available in the separate Java repository. This is a source-size cleanup, not a measured runtime/FPS/RAM claim.

## Rendering follow-up (beta 16)

Reviewed the Java edition's documented fitted-artwork, page-surface and cached-background fixes against current Compose callers. Retained the independent Compose implementation; no Java Views UI was copied back.

Removed or replaced in the active rendering path:

- Single-accent `LocalMatrixAccent`/theme subscription and instant per-focus surface tint changes.
- The `matrixBackdrop` per-frame gradient/Brush construction; static multi-color textures now have a four-entry cache and bounded transition endpoints.
- Fixed metadata-box focus borders and the unused black letterbox surface around fitted media artwork. `ArtworkFit` supplies the actual image bounds for clipping, rim and progress.
- Repeated app-picker first-row `scrollToItem(0)` requests on banner/focus updates; ordinary reveal plus initial entry focus handles this.
- Fixed Next-page and form-action footer regions; their actions now flow in the content list.
- Duplicated global/half-image sampling loops; app accents and media palettes share one bounded sampling helper.
- Unused explicit imports, stale build comments referring to unsupported package-name variants, and old single-color cache value types.

Shared replacements are `MatrixBackdrop`, `MatrixScrollEdges`/`MatrixScrollViewport`, `ArtworkFit`, `ArtworkPalette` and the existing Matrix card/style tokens. Stable row memory, settings revision triggers, account locks/cooldowns, device routing and optional services remain required. A low reference count alone is not evidence to delete them. This review covers the changed rendering/navigation path plus structural checks, not a claim of a line-by-line proof over every integration.

## Reorder and quality follow-up (beta 18)

Removed the editor's manual revision counter, index-based moving focus requester and direct lazy rendering of the core session's mutable ArrayList. It now publishes list snapshots, retains focus targets by ID and provides both OK-to-save and an explicit Save order action. Removed the raw-provider-order path for reopening the row editor; Home and the editor share the persisted ordering policy. Shared MatrixRowText replaces per-screen title/detail composition, with explicit typography line heights and rounded control/panel roles. The previous 256-pixel texture size and count-only cache policy were replaced by bounded 720p, float-precision textures and an allocation-byte budget.

The final viewport follow-up replaces lazy first-key anchoring with an explicit retained scroll position plus per-ID reveal requests. SIMKL source rows use a neutral vector list icon rather than the media launch target banner. Device save/reopen/cancel checks and the HD rendering cost are recorded in docs/VALIDATION.md.

## Control hierarchy and Home connection follow-up (beta 19)

Replaced label-only SettingAction rendering with semantic control roles shared by menu/header/form/editor callers. Removed the duplicated raw header Button implementation. Home recovery now uses the existing service lifecycle reference instead of querying the enabled-service list as if it proved a connection; unbind clears that reference. No new client, cache, UI framework or compatibility wrapper was added. Two focused reviews checked action callers and Home integration; validation scope and pending device checks are explicit in docs/VALIDATION.md.

## Typography, state and inspection follow-up (beta 21)

Three focused reviews covered TV typography/control hierarchy, stale/duplicate code, and official platform/Compose guidance. Removed obsolete file-wide experimental opt-ins, unused lazy/key imports, a test-only HomeCardSizing.columns wrapper, duplicate hardcoded app-picker column counts, changing On/Off prefixes from migrated menu titles, and parallel menu role/state arrays. The replacement is one typed MatrixMenuItem renderer with stable provider IDs, checked/selected semantics and existing shared style roles. The wrapper-only sizing test was replaced with coverage of the active contentColumns policy; useful boundary/dock coverage remains.

The typography scale is defined once instead of relying on implicit fallback weights/families; Home/idle clock callers use display roles instead of font-size overrides. Repeated RemoteConfig reads in Home settings composition moved into one async per-revision load. The safe Activity dump makes navigation diagnostics available without a new UI/test dependency. An integrated review fixed stale dump focus when leaving a row. A foreground binding check covers temporarily selected-but-unbound Home state after an APK update.

No further private helper deletion was justified by the audit. Remote mapping, optional integrations, manifest callbacks and preference revision triggers are retained. This is concrete cleanup plus shared UI/state improvements, not an exhaustive proof that every file is optimal or a measured FPS/RAM improvement. No DI framework, image loader, cache, polling service or dependency was added.

## Navigation follow-up (beta 22)

Removed unconditional app-grid/list scroll-to-zero return paths and replaced them with ID-based restoration. Removed the panel-scoped state-holder lifetime and added explicit popped-route draft cleanup. Removed the leftover consumed Home category that could reset recreated navigation. Centralized list Back decisions across the remote, header and dialog. Permission state replaces the static Allow TV listings row; Reload no longer borrows the emphasized commit style. Existing shared focus policy and safe diagnostics are extended, without a navigation framework or new dependency.

The final beta 23 follow-up adds explicit empty-list guidance instead of a blank content area, found during device navigation checks. Beta 22 was the intermediate navigation validation install; beta 23 is the final measured APK.

The final beta 24 clarifies manufacturer startup access and removes the new UseKtx warning. Reboot validation identified TCL AUTO_START denial for the new package, which was corrected as an authorized device setting with undo preserved; no additional receiver framework was added. The second reboot passed actual master-switch/binding/foreground checks. Full evidence and limits are recorded in docs/VALIDATION.md.

## Unified control appearance (beta 25)

Removed option-specific gray fill, the separate 16 dp option corner branch, transparent secondary/Back styling, role-dependent ordinary menu width/alignment and the Home toolbar's raw TV Button. MatrixControlStyle centralizes neutral shape, fill, stroke and padding; MatrixButton consumes it once for all roles. Form fields and loading shapes reuse that geometry, and the obsolete MatrixLayout.controlCorner token is removed. Commit icons/compact placement and destructive warning colors remain semantic distinctions. Added a structural guard against raw TV Button/ButtonDefaults imports outside the shared implementation. Media artwork/panel geometry remains distinct for its content purpose.

## Icon alignment follow-up (beta 26)

Removed the use of square text buttons for toolbar/header icons, the obsolete Back role and its special padding. Native TV IconButton now owns icon-only geometry in one MatrixIconButton wrapper; the text and icon wrappers share color/border builders and MatrixControlStyle. Normalized regular control glyph sizes, centralized header/toolbar frame sizes and extended the source audit to prevent raw IconButton styling in feature screens. Added read-only frame-alignment assertions to catch the actual defect that a shared-color audit missed. Content-specific poster, dock and panel layout is not incorrectly forced into button geometry.

## Beta 27 wallpaper retirement

Removed Reddit OAuth refresh, subreddit validation/forms, automatic polling toggle and provider-specific test. One-time compatibility code only removes retired preferences and reads the former image folder as an offline collection. Commons is an independent public source with no API credential fields. Shared import/rotation/encoding and Matrix controls remain the single implementations. Historical validation entries describe earlier builds and do not imply Reddit remains supported.

## Beta 32 row and border correction

Removed duplicated focus-rim paint, experimental inner artwork scaling, and the unconditional fit-change offset reset introduced in the earlier preview. Kept one shared artwork border renderer and one measured row-fit policy; portrait spacing preserves user-selected artwork size. Added read-only row geometry diagnostics to catch clipped slots without repetitive screenshots.

## Beta 34 whole-project audit

See docs/PROJECT-REVIEW.md for per-file coverage and changes. Removed the unused PersonalImages instance cache/loader, OpenOnStart implementation and obsolete tests, uncalled menu/power helpers, unread SIMKL notice tracking and unrendered subtitle formatting. Narrowed the legacy theme provider to the consumed remote config overlay. Production source changed by a net 152 lines removed; shared TV Material progress semantics and targeted correctness fixes remain.
