# theme-page

## ADDED Requirements

### Requirement: Token rows are editable by tapping
Each of the ten theme token rows (background, panel, surface, text, secondary
text, accent, corner radius, gap, focus zoom, tile size) SHALL be a tappable,
focusable row. Tapping a row SHALL advance its value to the next preset in
its list, wrapping to the first preset after the last.

#### Scenario: Tap focus zoom repeatedly
- **WHEN** the user taps the Focus zoom row
- **THEN** the value advances 1.000× → 1.015× → 1.030× → 1.050× → 1.080× →
  1.000×, and the row shows the new value

#### Scenario: Tap tile size repeatedly
- **WHEN** the user taps the Tile size row
- **THEN** the value advances 3 → 4 → 5 → 6 → 3 columns

### Requirement: A tap saves, applies, and restarts Home
Tapping a token row SHALL write the new theme to theme.json in the app's
files dir, apply it as the live theme, and restart Home, using the same
mechanism the Apply button already uses. The tile size SHALL also be written
to the LIST_COLUMNS preference so it takes effect.

#### Scenario: Change a value and leave settings
- **WHEN** the user taps a token row and then leaves the Theme page
- **THEN** the new value is already live on Home and survives an app restart
  (because theme.json holds it)

### Requirement: Every preset is a valid theme value
Every value in every preset list SHALL be within the ranges `parse` accepts
(radius 0–32, gap 0–32, focus scale 1.0–1.08, tile size 3–6) and SHALL keep
text/background contrast ≥ 4.5 and secondary-text/background contrast ≥ 3.0,
so a tapped value can never produce an ignored theme file.

### Requirement: Token rows are reachable by D-pad
Because the token rows are focusable, D-pad Up/Down SHALL move focus through
all ten rows and the three action rows, and the list SHALL scroll to show the
focused row. No token row SHALL be unreachable above the first focusable row.

### Requirement: Apply, Import, and Reset keep their meaning
- Apply SHALL re-read theme.json from disk, validate it, and restart Home.
- Import SHALL copy a picked JSON file to theme.json and apply it.
- Reset SHALL delete theme.json and apply the defaults.
- The page description SHALL explain that tapping a row changes its value.

## MODIFIED Requirements

### Requirement: Theme page rows are no longer display-only
The ten token rows SHALL NOT be `isStatus` rows. The ignored-file notice row
SHALL remain a display-only status row.
