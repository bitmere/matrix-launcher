# settings-dialogs

## Purpose
Bring settings, menus, and dialogs onto the glass system without hurting
legibility at TV distance — settings carry the most text in the launcher, so
they get the thickest, most legible treatment. The dialog window boundary
is resolved **here, in Phase 1 architecture**, not deferred.

## Current state (verified in code)
- One shared row shape: `SettingsPage` → `MatrixMenu` (LazyColumn of
  `MatrixMenuItem`s; status rows are plain text, never focusable).
- Controls use `MatrixButton` / `MatrixIconButton` / `MatrixTextEntry` with
  `MatrixControlStyle.fill` (onSurface @ 12%), `stroke` (light gradient),
  and `glass` (static vertical gradient, `MatrixButton.kt:59`).
- **Focus treatment today is not glow-only:** unfocused border is
  `Border.None` (2.7.15), but a focused ordinary button still has a 1.5dp
  onSurface border (`MatrixButton.kt:308–314`), and fields keep an
  unfocused stroke plus a 1.5dp focused stroke. Removing the remaining
  border needs an equally visible replacement, decided by the user (open
  question 5).
- All non-home pages (settings, search, lists, grids) live in a **separate
  Android `Dialog` window** (`MatrixHomeScreen.kt:152`,
  `DialogProperties(usePlatformDefaultWidth = false)`): a full-size 0.42
  black scrim with `MatrixPanel` inside. `MatrixPanel`
  (`MatrixAppearance.kt:24–33`) installs **its own**
  `MatrixBackdropSurface(panel = true)` provider — a fixed dark
  frost/sheen with a 1dp gradient border — not the shared glass source.
- No separate bespoke Update/Cancel Compose dialog was confirmed in 2.7.16:
  the About update/download/cancel/install states are a settings action
  row (`manual-update`) inside that page host. Do not plan against an
  assumed extra prompt class.
- Forms use `ClickableSurface` with the same fill/stroke tokens; changing
  `glass()` alone will not magically give the form path a blur.

## ADDED Requirements

### Requirement: Shared settings-dialogs contract
Matrix SHALL implement the following requirements across the staged runs.
- **Dialog boundary (Phase 1 architecture):**
  - FULL-tier enhancement: `FLAG_BLUR_BEHIND` + `blurBehindRadius`
    (API 31+, pixel radius) on the dialog window. It blurs the whole
    behind-window scene, not clipped to the inner panel; the 0.42 scrim
    stays on top. Support is **subscribed to** at runtime (listener
    registered, released on disposal) because hardware support, battery
    saving, multimedia tunnelling, and minimal postprocessing can disable
    cross-window blur while a page is open — a one-time API-level check
    is insufficient. While blur availability changes, contrast must hold.
  - **Fallback, always present from Phase 1:** wallpaper frost (the
    shared blurred-wallpaper bitmap drawn inside the panel) or an opaque
    panel at 0.85–0.95 base — named honestly as frost/panel, never as "a
    snapshot of what is behind". Never `PixelCopy` a fresh snapshot per
    dialog open. STATIC/CHEAP tiers always use the fallback. Treat
    video/tunneled playback behind the dialog as a fallback test case.
  - `MatrixPanel` consumes the hoisted shared glass source instead of
    installing its own synthetic provider. Keep a separate panel-local
    provider only where scroll-edge fading relies on panel pixels.
  - The panel is one material; rows inside it are thin overlays, not
    separate glass surfaces — making every menu row real glass inside a
    glass panel contradicts the no-stacking rule.
  - **Not chosen now:** moving settings/search out of `Dialog` into an
    in-window overlay. It would let Haze blur work there, but rewrites
    page hosting, focus/back handling, and IME behaviour. Deferred
    alternative, recorded with its cost.
- Settings panels and menus use control glass with the thick fill (highest
  opacity tier, base 0.85–0.95, tint ≤0.04–0.06, opacity 1.0 allowed for
  contrast / Reduce Transparency) so dense text stays legible. Status rows
  stay plain text with no container (unchanged).
- `MatrixControlStyle.glass()` becomes the real token (tint + edge light
  from the wallpaper palette) instead of its current static gradient; all
  buttons, fields, and menu rows inherit it — no per-screen glass code.
- Focus treatment on settings rows: decided by the user (open question 5 —
  coloured rim, existing border, or both under high-contrast mode). Until
  decided, the existing 1.5dp border stays; do not remove it speculatively.
- No changes to settings behaviour, preference keys, or navigation in
  Phase 1; D-pad order and focus restoration are unchanged. Verify on the
  TV: settings text legibility over light and dark wallpapers, dialog
  fallback readability with blur-behind unsupported, and the About update
  action row over a playing video.

## Delivery boundary
Run 1 implements source/policy/token foundations only. Run 2 adopts these on
surfaces and performs rendered/device validation; existing settings behaviour,
1.5dp focused borders and navigation remain intact. Persistent in-app motion
and transparency preferences are explicitly out of scope.

#### Scenario: Unmeasured device
- GIVEN a device without a measured GPU qualification
- WHEN selecting material quality
- THEN use STATIC wallpaper frost, or CHEAP for safe mode, low RAM,
  allocation failure or a nonpersistent low-effects choice
- AND do not enable live blur merely because API 31 is available.

## Run 2 implementation

`MatrixDialogBlur` registers the WindowManager cross-window-enabled listener with
main-executor delivery and removes it on disposal. API 31+ FULL applies
FLAG_BLUR_BEHIND with 18dp converted to pixels; support loss immediately clears the
flag and radius. Disposal restores the previous flag/radius. A late callback after
disposal does nothing. STATIC/CHEAP never enable the effect. Listener setup failure
uses fallback.

The shared source is sampled inside the panel using that dialog's own window bounds.
`MatrixBackdropState` remains panel-local solely to give scroll-edge fades the exact
same panel frost/fill pixels. It no longer generates synthetic panel textures.
Fallback is always drawn, independently of the listener, so tunneled video and
battery-saving support changes cannot remove the contrast floor. The .42 outer
scrim, navigation, fields, focus border and Android Dialog host are unchanged.
