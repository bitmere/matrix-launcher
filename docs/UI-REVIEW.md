# Settings consistency follow-up — local 1.1.1-beta.4

Reload Home was the sole settings menu row with a leading icon. Removed that exception, MatrixMenuItem's now-unused icon field/render branch/imports, and the root page's label-index/icon mapping. Home settings now uses the existing SettingAction/SettingsPage component, preserving row order, callbacks, the stable reload ID and loading guard.

MatrixButton explicitly provides labelLarge for standalone action labels; compact menu commit labels inherit that same style. MatrixRowText retains titleSmall/bodyMedium for setting title/detail hierarchy. Shape, padding, theme colors, focus scale and focus border already come from MatrixControlStyle. Commit icons and destructive colors remain semantic cues; app/media artwork, header/toolbar icons and toggle/radio state marks have different content roles and are retained.

Review covered root settings, shared menus/forms/buttons, integrations, Home controls, selection, ordering and wallpaper settings call sites. No raw pixel font overrides or alternate raw TV Button styling were found in those feature screens. This is a targeted consistency review, not a claim every visual state or device has been exhaustively rendered. Final optimized beta 4 installed with matching SHA256. Text dumps verify Reload focus, Integrations/TMDB/form navigation, Back and preserved Automatic media target/Home capture. Before/after settings screenshots confirm aligned text-only rows; a form screenshot confirms shared Cancel/Save typography and shapes. Reload was focused, not activated; no credential/save/toggle/media action. Automatic input stopped on Home. All 139 JVM tests pass; lint has no errors and its existing orientation warning. The shared structure audit and two parser tests pass. Historical beta validation; included in stable 1.2.0.

---

# Historical TV UI review - beta 21

This records the beta 21 review. For the current implementation, see [project review](PROJECT-REVIEW.md), [row/focus behavior](ROW-FOCUS.md) and [release validation](VALIDATION.md).

## Direction and review scope

Preserve the Apple-style media Home, remote-first navigation and lightweight TV Material 3 implementation. Three focused reviews covered typography/control hierarchy, dead/stale code, and Android/Compose platform practices. This is an actionable shared-path review, not a claim that every line or integration is proven optimal.

## Implemented

| Finding | Shared change |
| --- | --- |
| Titles/subtitles/buttons relied on similar implicit weights | One cached Outfit type scale: medium title/control roles, normal body, light display clocks. Removed Home/idle clock font-size overrides. |
| On/Off prefixes mixed value with title and shifted text | Typed checked/selected state and trailing markers. Checkbox/radio semantics remain independent of focus. |
| Role/state data used parallel menu arrays | MatrixMenuItem carries identity, text, command role and state together. Lazy keys/focus use stable IDs and content types use roles. |
| App picker exposed selection only as sibling text | Reuses the shared checked-state modifier on the clickable card. |
| Home settings parsed RemoteConfig repeatedly in composition | One async load per local revision; a loading state prevents acting on a placeholder config. |
| Dumps could report the prior row after focus moved to Back | Clear logical focus on row exit. Keep the item ID for return/restoration. |
| Logical navigation checks relied on screenshots after null XML | Activity's existing ADB dump exposes only page tokens, indices, generic states/counts and Home connection flags. No private account/media labels. |
| Stale selected Home state can need a rebind | One opt-in foreground binding check reuses recovery's existing permission, selection and concurrent-edit guards. No polling or service auto-selection. |

Removed obsolete opt-ins/imports, the unused columns wrapper and duplicate hardcoded picker counts. Kept sizing boundaries/tests, remote callbacks, manifest integrations and preference revision triggers. Their low reference count does not make them dead code.

## Research and decisions

- [Android TV typography](https://developer.android.com/design/ui/tv/guides/styles/typography): use meaningful type roles and readable branded type, rather than a different override on every screen.
- [Android TV buttons](https://developer.android.com/design/ui/tv/guides/components/buttons): action hierarchy and focus are distinct concerns. Existing commit pills and quieter navigation remain shared.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): expose headings, field labels and checked/selected state; changing only visible text is insufficient.
- [Compose performance practices](https://developer.android.com/develop/ui/compose/performance/bestpractices): stable lazy keys/content types and moving expensive work outside composition. No broad stability annotations or new framework were justified.
- [Activity.dump](https://developer.android.com/reference/android/app/Activity#dump(java.lang.String,%20java.io.FileDescriptor,%20java.io.PrintWriter,%20java.lang.String[])): use the platform's existing diagnostic path instead of a new exported debug component.
- [UiAutomation accessibility suppression](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES): automation can temporarily suppress helpers unless configured otherwise. A transient unbound state near a shell hierarchy dump is not proof of an update/startup failure. Wait for automation to exit and inspect actual binding again.

## Keep explicit

No new dependencies, DI framework, backend client, image loader, background service or polling were added. APK size and checks are in VALIDATION.md. There is no new measured FPS/RAM improvement claim. The previously measured HD backdrop rendering cost remains relevant. A proper observable preference model could eventually replace revision invalidation, but deleting invalidation first would risk stale UI. Physical key and reboot claims require separate evidence; logical dumps cannot replace those or visual clipping/color checks.

For future AndroidX instrumented UI tests, configure `Configurator.setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)` before obtaining UiDevice. The native shell dump has no equivalent documented keep-services option. This is a future harness note, not a new runtime dependency. For Home binding checks prefer the logical/window/service dumps, and recheck binding after any shell hierarchy dump exits.
