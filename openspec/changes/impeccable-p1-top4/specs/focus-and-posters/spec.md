## ADDED Requirements

### Requirement: Focused cards have a solid theme-driven outline and a brighter title

In addition to the existing glow and scale, a focused media/app card
SHALL show a solid focus outline (2dp, theme `onSurface` ring, using
the existing `MatrixLayout.focusOutlineWidth` /
`focusOutlineOffset` tokens) and its title SHALL brighten and bold.
`MatrixCardDefaults.border()` SHALL NOT return Border.None for the
focused state.

#### Scenario: Focus on a bright poster
- **Given** a card whose artwork is bright
- **When** it receives D-pad focus
- **Then** the solid outline and the brightened/bold title are visible
  against the artwork, independent of the glow.

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
