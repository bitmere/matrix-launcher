## ADDED Requirements

### Requirement: Focused cards rely on the artwork glow rim and brighter title without a second outer border

Focused media/app cards SHALL keep TV Material's outer `ClassicCard`
border at `Border.None` (`MatrixCardDefaults.border()`), relying on the
fitted `MatrixGlassBorder` artwork glow/rim, focus scale, and brightened
bold/medium card title (`cardTitleColor` / `cardTitleWeight`) instead of
adding a redundant white outer rectangular border around the whole card.

#### Scenario: Focus on a card
- **Given** a media or app card
- **When** it receives D-pad focus
- **Then** `MatrixCardDefaults.border()` uses `Border.None` for the outer
  `ClassicCard` container so no duplicate white outer border draws outside
  the glowing artwork rim, and the card title brightens and gains
  medium weight.

### Requirement: Posters distinguish loading from broken

`MatrixMediaCard`'s poster image SHALL show a token-driven
placeholder while loading and a distinct error state on failure, so
loading and broken are visually distinguishable. Cards with no image
URL keep the existing `ic_apps` fallback.

#### Scenario: Slow poster load
- **Given** a poster that has not loaded yet
- **When** the card renders
- **Then** the token-driven placeholder is shown, not the bare surface
  background.

#### Scenario: Failed poster load
- **Given** a poster URL that fails to load
- **When** the card renders
- **Then** the distinct error state is shown, visibly different from
  the loading placeholder.
