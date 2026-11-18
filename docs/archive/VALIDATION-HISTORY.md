# Historical device validation

These notes describe earlier builds and are not current release instructions. See [current validation](../VALIDATION.md).

# Local beta 32 border correction and whole-card spacing

Version 1.0.1-beta.32, code 59. Optimized APK 1,740,742 bytes; installed hash matches `1cbfa1fa895e1240b78bbcd71dc1d8d63724bb2ae795a2b9ba0ac38d136d0050`. Local only, not published.

Two subagents reviewed shared card paint and row geometry after the beta 31 visual regression. Removed experimental inner graphics-layer scaling and the broad translucent second stroke. Restored existing native TV focus scaling and corrected the single inset stroke radius to match artwork clipping. Removed the unconditional row-recreation scroll reset. Portrait rows preserve preferred artwork width and distribute spare space into gaps, while fixed-column dock/landscape rows retain measured widths. Remembered image requests, stable dock heading and skipping hidden gradient work under opaque wallpapers remain. See ROW-FOCUS.md for current behavior and sources.

130 JVM tests pass; zero lint errors, only the existing orientation warning. Structure and whitespace checks pass. Tests cover measured row fit, whole-stride reveal and preserving preferred poster dimensions; they do not establish painted border quality.

TCL checks verified the same focused SIMKL See all card at the right edge, leftward traversal, landscape row traversal and Favorites beyond the initial visible group. Row diagnostics reported zero partial slots at rest. Before/after captures were inspected: the gray duplicate inner rim is gone and the single rounded white rim follows artwork edges. Home capture remained connected and recovery enabled; no account/media or selection changes. The TV was returned to Home. No new frame-rate claim or exhaustive card/provider coverage is made. Screenshots and timing baseline remain ignored artifacts.

---

# Local 1.0.1-beta.30 Commons and local wallpapers

VersionCode **57**, optimized APK **1,740,738 bytes**. Installed base APK SHA256 matches `208921887c5550753097f6c7cc53a83eb0b5a2f64a84e1a905e7ed2d5e4d3249`. Local preview only; not published.

Removed Reddit network/OAuth flow, credential/subreddit forms and daily refresh settings. Upgrade removes the retired preference keys once, preserves selected wallpaper/rotation/account preferences and reads earlier downloads as a local archive without moving/deleting their files. Removed unused FeedRules subreddit policy/test. Current Settings and Appearance descriptions now name Commons.

Optional Commons featured landscape downloads reuse the existing bounded image decoder/JPEG storage and shared Matrix controls. Serial metadata/download work runs on IO behind the repository mutex, with five new pictures per batch, 20 cached pictures, 8 MiB transfer limits, daily opt-in checks while the launcher is visible and a one-minute manual cooldown. Each image has paired author/license/source metadata; credit rows open the existing shared QR page. Navigation uses hashed credit IDs so logical diagnostics do not expose image titles/URLs. No API key, new dependency, background download service, account/Home permission change or Java-repository edit.

127 JVM tests pass with no failures/errors. Lint has zero errors and only the existing TV orientation DiscouragedApi warning. Structural audit and diff whitespace checks pass. Regression coverage includes the official thumb.wikimedia.org and upload.wikimedia.org hosts, lookalike/HTTP rejection, minimum dimensions/aspect, attribution/license requirements, and idempotent preference retirement preserving account and wallpaper selections.

Real Commons API metadata and an HD image were verified from Windows, followed by Android HTTPS verification on the TCL. Initial device testing caught the newer thumb.wikimedia.org host being rejected; the allowlist and regression fixture were corrected. Five on-device downloads produced five JPEG/JSON pairs, totaling 2,101,672 bytes. All five cached images decode, preserve source proportions and are bounded to 1080 pixels high; every sidecar has author, license, original page and license URL. This is a maximum 1920×1080 cache, not an assertion that each image is exactly 1920×1080 or original 4K.

D-pad checks covered Wallpapers → credits → credit QR and Back restoring the selected credit/settings action. Final QR layout was visually inspected; author, license, modification notice and source link fit. Next wallpaper switched Home from the initial cached photo to another cached image. Daily Commons downloads was enabled only for this TV at the user's request; new shared installs remain opt-in. Home capture stayed connected, recovery enabled and five toolbar icons remained centered within 0.5 physical pixels. Final route-ID update was rechecked on the installed artifact; provider/cached images and settings survived the update.

Device inspection used logical activity/window metadata because TCL's UiAutomation hierarchy capture can suppress the selected accessibility helper. Screenshots were limited to download/credits/background rendering evidence and kept ignored. Temporary Android network-probe/capture files were removed. No reboot, Wi-Fi disconnect or 24-hour waiting test was repeated; daily cadence/offline fallback were reviewed in source, while actual downloads/cache selection are device evidence. No claim of measured FPS, whole-app memory or exhaustive UI coverage.

---

# Local beta 26 centered native icon controls

Version **1.0.1-beta.26**, versionCode **53**, optimized APK **1,740,742 bytes**. Installed base APK SHA256 matches `0b04f55813310dbe12a912047886f0da22b26fe297b5b4eb77663fa53da7f335`. Local preview only; not published.

The user identified the toolbar glyphs sitting too high inside their circles. Review covered shared text/icon buttons, header, toolbar, menu row icons, form controls and content-artwork boundaries. Pixel-bound inspection of the previous capture reproduced a common upward shift of approximately 10 pixels. A single visual style was present, but text-button row sizing/padding was incorrectly reused for icon-only content.

MatrixIconButton now uses native TV Material IconButton's centered layout. It and MatrixButton consume one MatrixControlStyle and shared color/border builders. Toolbar/header frame sizes and regular/inline glyph roles are defined centrally. Removed the obsolete Back role and text-button icon padding. Headers use 48 dp frames; toolbar uses 54 dp, regular control glyphs 24 dp. Menu inline icons use the same regular glyph token. Content-specific media/dock/panel geometry was reviewed and retained rather than forced into icon-button sizing. No vector assets were patched with per-page offsets.

Read-only IconLayout diagnostics report frame/glyph dimensions and center deltas, not private media/account data. Geometry is removed on disposal and does not write preferences or Compose animation state. tools/check_icon_layout.py verifies the focused Matrix window, resumed lifecycle, sufficient settled icons and center deltas within 0.5 physical pixels. The source audit now prevents raw TV IconButton imports in feature screens as well as Button imports. These checks target the actual layout bug, not just common color tokens.

123 JVM tests pass (24 core, 99 app), no failures/errors, zero lint errors and the existing orientation warning. Structural audit passes. One checked optimized release build completed in 79.6 seconds. No new dependencies or performance/FPS claims.

On the TV, all five toolbar icon frames measured **0.0 horizontal / 0.0 vertical offset** in ordinary and focused states. The shared header Back also measured zero offsets; checks passed on Home, Settings and Wallpapers. Final captures inspected actual painted glyphs, focused/unfocused controls, header alignment and retained common row styling. The earlier vertical displacement is corrected. Painted source glyphs have their own intrinsic optical whitespace, so frame assertions alone are not presented as proof of every vector's optical mass.

No media, account, wallpaper or preference changes were initiated. Home capture remains connected, recovery enabled, and TCL AUTO_START allow. No reboot repeated for this icon-only change; the beta 24 reboot and physical Home/Inputs/Settings confirmation remain startup evidence. Artifacts remain ignored. This is a shared-layout audit and representative visual verification, not exhaustive screenshot coverage of every feature/state/font scale.

---

# Local beta 25 one shared control treatment

Version **1.0.1-beta.25**, versionCode **52**, optimized APK **1,740,738 bytes**. Installed base APK SHA256 matches `814500b57811ecc4b4de47a9dc3cd6393c3c51956029a882045e450f2b542371`. Local preview only; not published.

The user confirmed physical Home, Inputs and Settings work after the beta 24 reboot, then identified inconsistent option/command styling. Removed the role-specific gray option fill/corner branch and transparent secondary/Back branch. MatrixControlStyle now owns neutral shape/fill/stroke/padding for every MatrixButton role. Menu choices and commands share the Import pictures treatment and ordinary row layout; commits remain compact with check icons and destructive actions retain warning colors. The Home toolbar's remaining raw TV Button was replaced by MatrixButton. Form fields reuse the same shape/tone/stroke, with their editing focus outline; neutral loading shapes use the same geometry. Removed the obsolete controlCorner token. Media artwork and panel geometry remain content-specific.

The structural audit rejects raw TV Button/ButtonDefaults imports outside MatrixButton.kt, catching the actual bypass found in this review. It is a source-boundary check, not proof of all rendered properties. Source search found no remaining raw TV Button imports in feature files.

123 JVM tests pass (24 core, 99 app), no failures/errors, zero lint errors and the existing TV-orientation warning. Final checked optimized build took 75.7 seconds. No dependency or runtime-performance claim was added.

On the idle TV, dump/window checks verified Settings -> Wallpapers -> wallpaper source choices -> Back -> Reddit sources form -> Back -> Home. Targeted captures verified consistent rounded outlined tonal options and Import pictures, header Back, form/Cancel/Save and Home toolbar icons. No import, wallpaper change, API credential edit or preference save was performed. Final diagnostics show connected Home, recovery enabled and listings access granted; TCL AUTO_START remains allow. No reboot was repeated for this style-only change; beta 24's successful reboot and subsequent physical remote confirmation remain the startup evidence. Artifacts remain ignored.

---

# Local beta 24 navigation, app returns and verified TCL startup

Version **1.0.1-beta.24**, versionCode **51**, optimized APK **1,740,738 bytes**. Installed base APK SHA256 matches `f05cd2e66092938be0dbb5c450c09a39b934796edd469238b5bd0753804836d5`. Local preview only; not published. Betas 22 and 23 were intermediate device-validation installs; the final beta 24 includes all navigation fixes, empty-list guidance, manufacturer startup help and removal of the new UseKtx lint warning.

## Implemented

- Reload Home uses a normal settings row with a refresh icon and stable identity. Refreshes while already loading are ignored. The Home channels page shows access status; Allow TV listings is offered only when missing. Permission state updates on activity start and permission result; previously blocked requests open app permission settings. The granted path was tested on-device; denial/permanent-denial paths were not exercised by revoking the user's existing access.
- The page state holder survives panel closure. Nested pages retain parent menu/grid focus, scroll and picker drafts. Popped routes discard their saved state, so cancelled drafts do not leak into a later visit. App/list grids restore focus by identity rather than always jumping to zero. The new regressions cover identity reorder/removal fallback and central list Back decisions.
- Header/remote/dialog Back share the same list policy: later pages or nonfirst cards return to page one's first card; the next Back pops to the actual parent. Empty lists now explain that they have no titles instead of presenting a blank page.
- Consumed Home requests remove CATEGORY_HOME as well as their action, preventing an old intent from resetting recreated navigation. External Home returns preserve the remembered Home position; another Home on the launcher resets to the top. The distinction tracks stopped-since-resume state, because onNewIntent itself may be delivered while paused. Media rows retain a card ID and use a safe default fallback when the historical card is no longer composed after data changes.

## Checks and device evidence

Focused navigation/Home-request/order tests passed before final checks. Final build: **123 JVM tests** (24 core, 99 app), zero failures/errors/skips, zero lint errors, one existing TV-orientation DiscouragedApi warning. Structural audit passes (13 manifest components, Android-free core). One intermediate R8 run hit a Windows output-file lock; retry reused cached successful checks. Final checked optimized build took 72.2 seconds. No new FPS/RAM claim is made.

Dumps drove logical navigation checks, with targeted final captures for visual state. Granted TV access shows Allowed and no permission action; Reload matches ordinary rows. Back from Home channels restored settings option 1. Favorites card 3 -> App options -> Back restored card 3; cancelling returned to settings without saving. A nonempty SIMKL custom list moved from card 0 to 1, Back returned to card 0, and the next Back returned to the same list choice in its actual parent. An empty built-in list was also checked and received explicit guidance in the final app. Pagination reset has policy-test coverage; later-page traversal was not repeated on-device in this pass.

Nuvio Home return preserved dock card 2, confirmed visually; another Home reset to dock card 0. Stremio launched without a media selection and Back returned to the same dock card 1. A settled Nuvio return preserved channel row 1/card 4; Back moved to that row's card 0, then Back returned to dock card 0. One earlier hot-task probe sent Home before the external transition had settled and reset to the dock; the accepted channel-return check explicitly verified the external window after a one-second settle. This does not establish frame-by-frame correctness for zero-delay app switching. No media playback, account/profile or source/favorite selection changes were initiated. The user subsequently confirmed physical Home, Inputs and Settings work after the final reboot. Injected checks alone were not used as physical-key proof. The existing held-volume one-step capture tradeoff remains.

## Startup diagnosis and resolution

The first authorized reboot failed: accessibility master became 0, Compose remained selected but unbound, and Google TV was foreground. Filtered TCL boot logs explicitly denied both Compose's HomeRecovery receiver and HomeKeyService with `callee_does't_have_OP_AUTO_START_permission`. Compose had `AUTO_START: ignore`; the retained Java edition had `allow`. This vendor permission did not transfer to the independent Compose package: a migration setup omission, not a receiver/Direct Boot defect.

Saved the prior Compose op mode for undo, changed only its supported AUTO_START op to allow, and persisted app-op settings. User approved a second reboot. The event log confirmed that Compose was started for its HomeRecovery boot broadcast. After that reboot, without manually launching Matrix through ADB: AUTO_START remained allow, accessibility master was 1, Matrix Launcher Home control was bound, crashed-services was empty, and Compose was the focused Home activity. The logical dump showed Home, recovery enabled and service connected; a final screenshot confirmed Matrix Home. The original Java helper stayed unselected and the original app/accounts/preferences remained installed. Restore script now also restores AUTO_START to its prior ignore mode. Debugging remains enabled at the user's prior request.

Device-specific setup/undo guidance is in README and ARCHITECTURE. No hidden vendor API or unconditional AUTO_START assumption was added to the app. Dumps, receipts, screenshots, temporary inspection scripts and vendor-state backups remain in ignored artifacts. No publication was performed.

---

# Local beta 21 typography, semantic choices and cleanup review

Version **1.0.1-beta.21**, versionCode **48**, optimized APK **1,724,354 bytes**. Installed base APK SHA256 matches `5955ed4d73b373824a32249610ab5207623715a0ccc8cdf42408b25e0f29cd25`. Local preview only; not published. See UI-REVIEW.md for findings, decisions and primary research references.

Three subagent reviews covered typography/control consistency, dead/stale code and official platform practices. Shared title/body/control weights and family roles are explicit; Home clocks retain their light display scale. Typed menu rows carry stable IDs, action roles and checked/selected state; changing On/Off no longer changes their titles. Headers expose heading semantics, fields expose labels, and app-picker cards expose checked state. Menu focus loss clears the diagnostic index; current logical state is available through Activity.dump without private labels or credentials. Home settings parse RemoteConfig once per revision off the main thread.

Actual cleanup removes the obsolete columns wrapper, redundant picker constants, unused imports/opt-ins and stale setup comments. Useful sizing tests remain, including direct coverage of the real contentColumns policy. No dependency, framework, cache, client or polling loop was added. Removed code is enumerated in ../CLEANUP.md; this does not claim exhaustive optimization across every line.

Focused checks preceded final release checks. An adapter compile mismatch and two comment-encoding errors were corrected before the accepted final build. Beta 20 was an intermediate installed candidate; review then corrected stale logical focus and added the guarded foreground recovery check. Final beta 21 passes **120 JVM tests** (24 core, 96 app), zero failures/errors/skips, zero lint errors and the one existing TV-orientation warning. Structural audit passes (13 manifest components, 67 app classes including the small inspection holder, Android-free core). Final checked optimized build took 70.8 seconds. No new runtime speed claim was measured.

Authorized TV checks: installed exact optimized bytes; confirmed actual Home binding and existing recovery opt-in/grant; dumps tracked settings row 0 to Back (-1/none) and back to row 0. Opened SIMKL library choices and observed focused state On -> Off -> On in the dump, then cancelled without saving. Card-size page showed focus on unselected choices independently of selection; no size was changed. Repeated injected Home presses returned to Home with the Compose window focused. Final targeted captures verified medium-weight titles, normal supporting text, unchanged light Home clock and trailing checked/unchecked markers. XML was available once on the intermediate build and its focused node/bounds were parsed; this is not a blanket XML assertion pass for all screens. No media playback, profile/account/source selection, default/helper or permission changes were made by the final checks.

An intermediate diagnostic observed a selected-but-unbound helper near UI automation and an APK update; it later reconnected. Official UiAutomation documentation shows that automation can suppress accessibility services. This observation alone therefore does not establish an APK-update root cause. The new foreground check is bounded and opt-in, and final dumps independently confirm binding after automation has exited. No helper is added if the user deselects it. Screenshots/dumps/receipts stay ignored.

The prior physical Home/D-pad confirmation and one-step held-volume TCL tradeoff remain recorded for beta 19; physical keys were not re-tested in this pass. Startup recovery after a reboot remains unverified. The earlier HD-gradient timing/memory caveats remain applicable.

---

# Local beta 19 control hierarchy and Compose Home takeover

Version **1.0.1-beta.19**, versionCode **46**, optimized APK **1,724,354 bytes**. Installed base APK SHA256 matches `dd35dd9e3b9f549c9f3a9456ae2bb70762fcabfe6d1e60aacdf582018a3bf5f3`. Local preview only; not published.

Two focused read-only reviews covered shared controls/callers and Home capture/recovery. SettingAction now carries a semantic role through SettingsPage and MatrixMenu. Save rows/lists and editor/form/favorites commits use compact check-mark pills, options retain the rounded full-width row, Back is a quiet icon, Cancel/page navigation are secondary, and destructive confirmations use error roles. Immediate commands are explicitly marked rather than inferred from labels. MatrixPageHeader no longer duplicates raw button styling. No framework or dependency was added.

HomeRecovery previously used the enabled-service list as proof of a bound service. It now reuses the service's real process-local connection state, cleared on unbind/destroy, so stale selection cannot suppress the recovery attempt or incorrectly show Active. Remote mappings, input routing and controller passthrough remain in the capture path.

Focused HomeKeyService/HomeRows/RemoteKeys regressions passed first; the final checked optimized build then passed all 120 JVM tests (24 core, 96 app), zero lint errors and one existing TV-orientation warning. Structural audit passes: 13 manifest components, Android-free core. One final release build took 75 seconds.

UI metadata was attempted first with ordinary/compressed dumps; this TCL returned null roots. Window/service dumps established foreground package and actual binding. Targeted final captures verified the Save rows check-mark pill separately from options, quiet Back, connected Home status and startup recovery On. Library/source/account selections were not changed. An initial settings navigation reused remembered focus and opened TV network settings; no network values were changed. Navigation was reset through Home before the accepted final checks.

With user authorization, only the original Java Home accessibility component was replaced by the Compose Home component, preserving unrelated entries. The Compose service is bound as **Matrix Launcher · Home control** with key-filter capability. WRITE_SECURE_SETTINGS was granted and startup recovery enabled through the UI. The default-Home command printed Success, but resolving native Home still returns Google TV; the accessibility override provides Compose Home behavior. Eight repeated injected Home presses ended in Compose. The old Java app/accounts/preferences remain installed and retained; Google TV remains enabled. Baseline and an exact restore script are in ignored artifacts. The script restores the old service selection/master switch/native Home and revokes the new grant; Compose's recovery flag remains dormant because its helper is removed and permission revoked.

The user confirmed that repeated physical Home presses and D-pad navigation work, while held Volume Up moves one step. This confirms the TCL key-filtering tradeoff remains on the Compose capture path. Home capture remains enabled, consistent with the previously chosen preference; no volume workaround was enabled. A reboot was not performed in this pass, so startup recovery is enabled but its post-reboot behavior is not yet verified. The prior HD backdrop timing limitations still apply. This is a focused shared-control/Home-path audit, not a claim of exhaustive proof over every integration.

---

# Local beta 18 reorder, shared controls and HD backdrop

Version **1.0.1-beta.18**, versionCode **45**, optimized APK **1,723,822 bytes**. Installed TV base APK SHA256 matches `e20100743b7d1f6bdaac232118bb3ef726fa79dde3d4094d0ade8fa610456c35`. Local preview only; not published.

Reorder now publishes immutable list snapshots and retains focus/reveal targets by item ID. A move preserves the viewport position instead of anchoring it to the old first visible key. OK saves the active move; an explicit Save order action is also available. Back cancels the active move. Home and the reopened editor share persisted ordering, including hidden rows in the editor. Removed the manual revision/index-focus path and raw provider-order editor path.

Shared MatrixRowText supplies aligned title/supporting text and focus-aware contrast. Controls use the shared 16 dp corner role; right panels use 24 dp outer corners. Typography has explicit line heights. Reorder uses vector move/list icons; SIMKL rows no longer inherit the Nuvio playback target's banner as their source identity.

Backdrop textures now scale proportionally up to 1280 x 720 using RGBA_F16 precision, replacing the old 256-pixel ARGB8888 raster. Cache eviction uses a 16 MiB allocation-byte budget. Active transition endpoints/snapshots and other app allocations are additional memory. Existing off-main generation, three ambient lobes and bounded crossfade endpoints remain. No noise layer, blur, polling or dependencies were added. This is higher-precision raster rendering, not native 4K or HDR output; the display/compositor can still introduce banding.

120 JVM tests pass (24 core, 96 app), with zero failures/errors/skips. New coverage checks saved editor/Home ordering including hidden rows, proportional texture dimensions and the cache size policy. Lint: zero errors, one existing TV-orientation DiscouragedApi warning. Final checked release build completed in 78.46 seconds.

Real TCL checks verified: moving Nuvio from position 1 to 2 retains focus on Nuvio and leaves the preceding row visible; Back cancels the move; OK saves; leaving and reopening preserves the new order; Right reaches the visible Save order action and saves successfully. The original visible sequence was restored through Save order, with Nuvio first. Rounded controls, title/supporting-text alignment and multi-color backdrop were visually inspected. XML was attempted but the TCL returned a null root, so these are foreground-window/D-pad/screenshot checks, not XML assertion passes. No playback or account/profile/source-visibility changes. The original Java Home service remains selected.

A guarded warm Nuvio-row sample with 24 paced left/right moves recorded **605 frames, 18 janky (2.98%)**, median **36 ms**, p95 **42 ms**, 3 slow bitmap uploads and 12 slow draw-command issues. Legacy jank was 442 frames (73.06%); the metrics use different definitions. The beta 16 sample had median 26 ms and p95 40 ms, so this does not establish a speed improvement: the HD path has rendering cost and occasional misses remain. This short single-device sample is not a universal FPS claim or a controlled repeated benchmark. Warm capture and reports stay in ignored artifacts.

---

# Local beta 16 rendering parity and motion review

Version **1.0.1-beta.16**, versionCode **43**, optimized APK **1,722,818 bytes**. Installed TV APK SHA256 matches `fc9d0b6d3ade603b1c9bcef9a5716ab537999480a5d0829c199defa58e224cb7`. Local preview only; not published.

Compared the preserved Java edition's project-audit, card-page and validation notes against current Compose behavior. Implemented the missing behavior with Compose/shared Android rendering adapters rather than reintroducing Java Views UI:

- Fitted image bounds own clipping, focus rim and inset progress. Variable-height posters leave transparent layout space instead of a decorated letterbox; the source remains proportional with no center crop. Black pixels already embedded in source artwork are retained.
- Cards share a restrained 1.015 focus scale and a short fading rim with a crisp inner edge. Obsolete per-focus theme tint updates and app-picker first-row jump requests are removed.
- Two artwork colors feed three cached ambient lobes. A four-entry small texture cache replaces frame-by-frame gradient allocation. A transition retains two endpoints; interruption snapshots the current blend off the main thread instead of nesting transitions. Readable theme surface roles stay stable during focus changes.
- Headers stay pinned over the same page backdrop. Only 24 dp scroll-edge strips are composited from that exact backdrop. The shared vertical reveal policy reserves the fade plus focus clearance so a focused option is not dimmed. Horizontal Home rows retain their existing fixed margins.
- Next-page and Cancel/Save actions flow after content instead of occupying a fixed footer. IME Next/Done scrolls and focuses the target field/action after it is composed.

118 JVM tests pass (24 core, 94 app); new coverage includes proportional bounds for varied source aspect ratios, invalid dimensions, app palette identity and focus clearance outside the edge fade. Lint: zero errors, one existing TV-orientation warning. Structural audit passes (13 manifest components, Android-free core); inspection/profiling helpers compile. See ../CLEANUP.md for specific removed paths and the scope of the review.

Real TCL checks: Nuvio landscape progress/rim, SIMKL portrait row, list top/middle/end, pinned header soft edge, fully visible focused settings option, reachable flowing Next-page button, and two-field API form Next/Done reaching Save. Form values were left empty and cancelled; no configuration was saved. No playback, input, profile, account, permission or Home-helper changes. The original Java Home service remains selected. XML was attempted but returned null roots; these are D-pad/window and screenshot checks, not XML assertion passes. The TV's system dream overlay was explicitly dismissed before accepted captures. Bulk unpaced injections can outrun lazy composition; final list-end checks used paced presses.

## Performance evidence and limits

Official Perfetto helper traces and gfxinfo snapshots are stored in ignored artifacts. The guarded final sample opened Home, confirmed the focused package, captured the warm Nuvio row, then sent 24 paced left/right moves. It recorded **497 frames, 27 janky (5.43%)**, median **26 ms**, 95th percentile **40 ms**, and one slow draw-command issue. In its trace, 202 main-thread Choreographer frames had a maximum duration of 23.1 ms. These are device/sample results, not a universal FPS promise; occasional misses remain.

The earlier exploratory beta 14 run recorded 322 frames, 125 janky (38.82%), median 42 ms, 95th percentile 117 ms and 125 slow draw-command issues. Its starting route was not captured and it lacked the final dream/focus guard, so these figures are **not a controlled A/B percentage improvement**. Vendor GPU percentiles in that run were implausible and are not used. Windows Perfetto daemon/remote-session lookup was unavailable; small traces were queried directly after inspecting schemas. The local analysis process was stopped after use.

Prior account, provider-profile, physical Home-capture, startup/reboot and device/font-scale limitations still apply. Screenshots, traces and account/device artifacts stay out of source control.

---

# Local beta 14 row alignment, panel separation and compact posters

Version **1.0.1-beta.14**, versionCode **41**. Optimized APK **1,722,822 bytes**, installed and checksum-verified. SHA256: `5327ddb53d189e577afbf62a5a9a1eef5af9cb7635a41e36d809b443b986be38`. Local preview only; not published.

The baseline TV capture reproduced the first-card bug: ordinary D-pad return let the channel's scrolling content padding collapse toward the screen edge. MatrixFocusRow now places the alignment margin outside the scroll viewport and shares the theme's 8 dp reveal inset inside it. Both the dock and channel rows use this component; Back is not required to restore alignment.

Portrait cards use narrower, capped sizing while retaining 2:3 artwork; landscape cards retain 16:9 and their current size. The same sizing policy feeds Home rows and full lists. Portrait-only grids fit columns to measured available width instead of stretching posters to landscape slots; mixed-orientation grids center each card in its slot. Existing size preferences still apply. All images retain Fit scaling.

Panels now use an opaque neutral dark base tinted by focused artwork, with soft vertical lighting and left-edge shading. The same panel/backdrop and semantic surface roles apply across settings and list pages, without blur, extra dependencies or separator lines.

114 JVM tests pass (24 core, 90 app). New regressions cover fixed row/reveal margins and portrait-grid fitting across every size choice. Lint: zero errors, one existing TV-orientation warning. Structural audit passes (13 manifest components, Android-free core).

Real TCL checks: moved right and left back to the first Nuvio landscape card and SIMKL portrait card; the first card stayed at the row-title inset. Scrolled the favorite dock to both ends and returned to its first card; its inset and focus outline remained inside the band. Opened SIMKL See all without opening media; the current size fits six portrait columns with full artwork and centered captions. Navigated the list grid back toward its header and inspected the right-side settings panel against Home. Targeted screenshots verify alignment, poster size and panel separation. XML capture was attempted but returned null roots; these are visual/D-pad checks, not XML assertion passes.

No preferences, accounts, profiles, playback, input selection, permissions or Home-service choices were changed by these checks. The preserved Java Home service remains selected. Screenshots and device receipts remain in ignored artifacts. Prior migration/device limitations still apply.

---

# Local beta 13 shared navigation and appearance review

Version **1.0.1-beta.13**, versionCode **40**, installed and SHA256-verified on the TCL. Optimized APK: **1,706,398 bytes**. SHA256: `21318d075eb8ad22eda7befa74b75cc69a0c7962f31583f36a6e23e5bb665aa9`. Local preview only; not published.

Shared MatrixPageHeader replaces oversized Back footers across settings, app/source/favorite pickers, reorder, SIMKL QR/status/list pages and licenses. Actual parent history supplies breadcrumbs; physical Back keeps existing page-one, cancel-move and parent semantics. Forms retain compact Cancel/Save actions. Header/content share one background without a divider.

MatrixAppearance centralizes the focused artwork seed, the two ambient gradients and panel geometry. Home, the app picker and media-list focus feed the same state. Opaque dark, lightly tinted panels prevent underlying text bleeding through. Sampling uses the existing repository cache and cancellable focus effect, with no per-focus preference writes, new image pipeline or background polling. Form fields now use the shared small focus zoom and a readable dark fill. The obsolete panel-color resource was removed.

112 JVM tests pass (24 core, 88 app), including actual-history breadcrumb regressions. Lint: zero errors, one existing TV-orientation warning; no unused-resource warning. Structural audit passes (13 manifest components; Android-free core). The XML helper recognizes accessible icon descriptions as well as text.

Real-TV checks used D-pad navigation and attempted XML first. TCL repeatedly returned null roots for dialogs; these are visual/window checks, not XML assertion passes. Reviewed main settings, favorite grid and cancellation, channel reorder header, connected SIMKL menu and the non-auth account-management QR, unsaved Reddit form, Inputs and internal Quick Settings. A disposable text value was readable in the field and cancelled without saving. A canceled favorite toggle reverted on reopening. Focused app changes update the ambient tint. No input, playback, account/profile, Home helper or device permission was changed. The original Java Home service remains selected.

Quick Settings is deliberately non-exported: direct shell launch is refused; its internal Remote Settings entry was used successfully instead. Some first TV captures repeated earlier frames; current-window checks, D-pad movement and a preview-process restart were used before accepting rendering evidence.

All supported Matrix screens have Compose implementations; this is not full Classic-addon parity. This review does not certify every content page, fresh SIMKL reauthorization, authenticated Reddit downloads, provider profile changes, physical Home capture, boot recovery or all font scales/devices. Existing migration limitations still apply. Device/account captures remain ignored local artifacts.

---

# Local beta 11 SIMKL connection follow-up

Version **1.0.1-beta.11**, versionCode **38**, installed and checksum-verified on the TV. This correction is not published. SHA256: `0ffe71e5752a00fecaf92f3fc8e9777edb3aa4f9efef16f5e2e7d84a0a07ee85`.

The user completed SIMKL approval successfully on beta 10. The UI then popped back to its Client ID form, even though the account was connected. Beta 11 replaces the completed setup branch with the connected SIMKL menu, shows a success toast and persistent connected description, and skips device-code creation when the account is already connected. Same-ID setup submission also returns to the connected menu.

110 JVM tests pass (24 core, 86 app), including navigation regressions for approval from both Client ID and existing-ID entry. Lint: zero errors, one TV-orientation warning. On the TV, the existing login survived installation and the new connected account description was visually verified. No sign-out or repeat authorization was used to test the correction; the success-branch history is covered by JVM tests.

Shared forms now route keyboard Done/Next and D-pad Down to the next field or Save. Real-TV checks on an unsaved, empty API form confirmed that after closing the on-screen keyboard, D-pad navigation reaches Save. An incidental test character was erased and the form was cancelled; no Reddit credentials/preferences were saved.

Account identifiers, tokens, QR codes and device captures remain in private app storage/ignored artifacts, never source control.

---

# Local beta 10 TV follow-up

Version **1.0.1-beta.10**, versionCode **37**. This preview is installed on the TCL as a separate Compose app and is not published to GitHub yet. Optimized APK: **1,705,938 bytes**. SHA256: `02767c75638f96529feb9c78ffeeddead30eadf3d53f10f92cf14e2e8df14a1b`; the installed TV APK matches.

- 108 JVM tests pass (24 core, 84 app); lint zero errors, one TV-orientation warning. New regressions cover focus-safe insets and oversized reveal targets.
- Real TV: Nuvio's published channel is visible. Back from a later media card returned to the first card. Returning to Favorites restored the full Home header (`WELCOME HOME` begins at y=68 in the 1080p UI). The app picker opens/returns to its first row. Visual checks confirm image-only focus outlines, centered captions/state labels, aligned header actions and smaller rectangular menu buttons.
- Shared reveal behavior now covers menu/list grids, not just Home. Home/favorites/header focus restores the top explicitly. Standard actions share compact MatrixButton styling; circular Home tools remain separate.
- Local paste helper tested using `MatrixPasteTest123` in an unsaved public-ID field. The value was visually verified, then erased without saving or making an auth request. The helper correctly refused the non-editor Home screen.
- The original Java Home accessibility selection is unchanged. Compose source/favorite choices survived the app update. No media was opened and no account/profile was changed for the checks.
- TCL hierarchy capture sometimes fails or returns underlying Home for dialogs; those cases were verified visually, not recorded as XML passes.

Actual SIMKL/Reddit account connection still requires the user's values/phone approval. Input switching, Compose Home takeover/reboot recovery and physical remote capture have not been certified in this beta. Those limitations from beta 9 remain unless explicitly covered above.

---

# Beta 9 validation

Version 1.0.1-beta.9, Android versionCode 36. Measured with the then-current package. Optimized universal APK: **1,705,934 bytes (1.71 MB decimal)**, versus 2,023,635 bytes for the mixed Compose prototype (15.7% smaller). Java checkpoint: 667,141 bytes. Size is not a runtime memory or speed measurement.

SHA256: `b57cd0c935f751711436de10edac1091e386cdfa12318cb3b5e28410628c8fe1`.

- 106 JVM tests pass: 24 core, 82 Android backend/policy tests; no failures/skips.
- Lint: zero errors, one warning about fixed TV orientation on future Android versions. The launcher remains landscape for its intended TV use.
- Structural audit passes: 13 manifest components, no retired Java Home/grid UI, core has no Android imports. Home, Inputs, remote app launch and accessibility metadata are declared.
- Unminified emulator preview: 21 XML/D-pad screen assertions pass, covering Home/settings, sources/favorites cancellation, appearance/idle-clock menu, Reddit source/API forms, optional SIMKL setup, Home controls, licenses and Back history. No credentials, real playback or selected app/channel lists were changed by these checks.
- Final optimized APK installed on emulator-5580; installed base.apk SHA256 matches the measured artifact. Nine Home/settings/source/favorites/Back assertions pass on those exact bytes. Representative Home/settings rendering was inspected visually.
- Final source changes add regression coverage for preserving saved navigation on process recreation. Provider callbacks are ignored once Home has stopped. App card accessibility names avoid duplication with visible captions.
- The TV was not changed by this refactor. This beta does not replace the installed Java Home/helper or import its private account data.

Not certified by these checks: authenticated SIMKL/Reddit flows in the new sandbox, actual provider/profile refresh, real TV input switching, clock/dream lifecycle on vendor firmware, boot recovery, physical Home/volume behavior, FPS/RAM/startup comparisons. The emulator is TV-shaped but runs a phone system image. Enable helpers only during an explicitly authorized real-device test, with one launcher override active at a time.

A Windows DEX file lock interrupted one R8 run; an unchanged-source retry succeeded. Configuration cache reuse was verified. A successful optimized build with checks took 58.3 seconds on this host after the correction; this is not a cold-build guarantee or a controlled comparison with Java.
