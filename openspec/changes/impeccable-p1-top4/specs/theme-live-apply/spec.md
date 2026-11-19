## ADDED Requirements

### Requirement: Theme changes apply live, without an activity restart

Tapping a theme token row, pressing Apply, or resetting the theme
SHALL apply the new `ThemeState` live in place through the
already-observable `MatrixLayout.theme`. The theme settings path
SHALL NOT call `scope.recreate()`. When `tileSize` changes, the page
SHALL trigger the same data refresh the Size page uses
(`refresh(false)`) instead of a recreate. `recreate()` remains only
where a value genuinely cannot apply live, and any such case SHALL be
documented in the change.

#### Scenario: Cycling presets
- **Given** the user is on Settings → Theme
- **When** they tap token rows 4 times in a row
- **Then** the Home colours/radius/gap/scale update live 4 times,
  the activity is not restarted, and focus stays on the theme page.

#### Scenario: Tile size changes
- **Given** the user changes the tile-size token
- **When** the new state applies
- **Then** the grid re-lays out via the Size page's refresh path,
  with no activity restart.

### Requirement: Safe-mode behaviour is untouched

The safe-mode `recreate()` calls in `MatrixSettingsPages.kt` SHALL
remain exactly as they are; this change touches only the theme path.
