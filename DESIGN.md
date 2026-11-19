# Matrix — design system

Recorded from the existing code and README. Nothing here is new: this
file describes the system as it already is, so later work follows it.

## Tokens

Theme tokens live in `app/src/main/java/com/matrix/launcher/MatrixTheme.kt`
and are user-editable through a validated `theme.json` file with JSON
import, export, and reset. Defaults:

- Colours: background `#000000`, panel `#1C1E24`, surface `#242B34`,
  text `#FFFFFF`, text secondary `#D1D5DF`, accent defaults to text.
- Geometry: corner radius 24dp, gap 16dp, focus scale 1.015, tile size 5
  (range 3 to 6), focus inset 8dp.
- Typography: Outfit (OFL, weights 300/400/500), one family across the
  TV Material 3 scale — displayLarge 76/84 Light down to labelSmall
  11/16 Medium.
- The theme maps onto TV Material 3 `darkColorScheme`: primary = accent,
  onPrimary = black, background, onBackground = text, surface = panel,
  surfaceVariant = surface, onSurfaceVariant = text secondary.
- Contrast rules enforced by the theme parser: text/background at least
  4.5, text secondary/background at least 3.0. A bad `theme.json` is
  ignored whole (defaults + a reason shown on the Theme page).
- Ambient colour is derived from the current artwork. Wallpapers come
  from storage, Wikimedia Commons landscapes, or artwork colours, with
  offline rotation at 15, 30, 60, or 120 minutes.

## Components

- Shared controls: MatrixButton and MatrixControlStyle for settings
  options, commands, header navigation and toolbar buttons; MatrixIconButton
  for icon-only controls; MatrixRowText for settings titles and details.
  Settings rows align on a shared leading text edge, no isolated leading
  icons. Check icon centering with screenshot pixel bounds after layout.
- App grid cards, 4, 8, or 12 cards per row. Rows are reorderable and
  can be shown or hidden. Favorites are reorderable.
- Channels, search, and lists follow the same card grid with page-by-page
  pagination.
- Settings is grouped. Focus, selection, and the Update/Cancel prompt
  must stay legible at TV viewing distance.

## Rules

- Decide tokens first. Work inside the tokens above. Do not invent a new
  palette or typeface as part of a refinement.
- The no-ai-slop-design skill governs the banned list for this project.
  Run the Impeccable review passes (critique, layout, typeset, simplify,
  clarify, polish, harden, audit) on UI changes, and run the detector
  before calling UI work done.
- Kotlin files are not in the detector's built-in extension list. The
  project config adds `.kt` with the text engine so the hook covers them.

## Skill resolution order

Project-local skills first, then user-level skills, then the ai-config
repo index as reference only. Never use a stale skill: if a skill is old,
leave it out and say so.
