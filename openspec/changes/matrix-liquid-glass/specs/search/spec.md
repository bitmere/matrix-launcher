# search

## Purpose
Make search feel like the fluid entry point: a glass search field, and
results that settle with spring motion instead of appearing in a block.

## Current state (verified in code)
- `MatrixSearchScreen.kt` (833 lines): search field, orb, results grid
  reusing the same card grid as Home.
- **Search lives inside the settings/search `Dialog` window**
  (`MatrixHomeScreen.kt:152`). The field cannot sample Home pixels through
  the dialog simply by inheriting a local — its glass treatment comes from
  the dialog host resolved in `settings-dialogs`, not from Home.
- Entrances today: a 350ms result-container fade/slide and a shared
  AnimatedContent transition (`tween(220, delayMillis = 40)`). The 40ms
  delay is a **shared transition delay, not demonstrated per-result
  staggering**.
- Orb rotation uses infinite `tween` animations; an additional loading
  pulse/rotation exists. Orb and loading motion are **not** universally
  capability-gated.
- Results use `MatrixMediaCard`, so the card frost treatment (Phase 2)
  applies here automatically.

## ADDED Requirements

### Requirement: Shared search contract
Matrix SHALL implement the following requirements across the staged runs.
- The search field becomes control glass via the dialog host treatment
  (blur-behind enhancement where supported, wallpaper frost / opaque
  panel fallback otherwise — see `settings-dialogs`), with the text-entry
  tokens from `MatrixTextEntry` (same fill/stroke, glass variant).
- Result entrances move to the shared spring family from `focus-motion`:
  stagger 20–30ms is a candidate (cap total delay 120ms), applied only to
  the initial visible group; the focused destination appears immediately;
  entrances are interruptible when the query changes mid-animation. Stable
  item identity prevents animation restarts on streamed results. Never
  delay focus or input for an entrance.
- The orb keeps its current rotation (linear rotation is correct for an
  orb); the only addition is the contour-glow treatment from Phase 3 —
  "Contour Glow" is a reported label from secondary coverage, **not
  confirmed as an official ColorOS feature name** — reusing the orb's
  existing rotation angle, with no new infinite transition. The existing
  orb/loading loops are gated (reduced motion / CHEAP) before any new
  effect is added; a static recognizable orb remains in reduced-motion /
  CHEAP states.
- No change to search behaviour, ranking, pagination, or providers
  (Cinemeta / TMDB / SIMKL). Verify on the TV: field legibility over the
  wallpaper, and no jank when results stream in while the user is typing.

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
