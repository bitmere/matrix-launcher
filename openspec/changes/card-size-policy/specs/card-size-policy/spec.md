## ADDED Requirements

### Requirement: One shared preferred portrait width for Home, Search, and list pages

Home media rows, the Search results grid, and the list page SHALL derive
the portrait poster frame width from one shared pure calculation taking
the measured window width and the `list_columns` value (3–6). The
calculation SHALL use one common sizing inset (`channelInset`) on all
three screens, and each container SHALL then fit that width into its own
layout. The same window width and columns SHALL produce the same poster
width on every screen.

#### Scenario: Same size setting, both screens
- **Given** the Card size setting is Compact (5 columns) and the window is 960 dp wide
- **When** the same title renders on a Home row and in Search results
- **Then** both posters are 99 dp wide

#### Scenario: Container fitting does not change the poster width
- **Given** the shared calculation returns a 168 dp poster for Large on a 960 dp window
- **When** the Search panel (narrower than the window) fits it into its grid
- **Then** the poster stays 168 dp and the column count adapts, unless the container slot itself is narrower

### Requirement: Four-size portrait policy with viewport-scaled ceilings

The single 144 dp ceiling SHALL be replaced by one ceiling per size,
scaled by the window width: base ceilings at the 960 dp reference window
are Large 168 dp, Medium 144 dp, Compact 120 dp, Small 96 dp, and the
scale is `(windowWidth / 960).coerceIn(0.75, 1.25)`. The rendered poster
width SHALL be `min(slotWidth × 0.625, ceiling)`, so each size step is
visibly distinct at every viewport and no poster overflows the viewport.
The pref key and values (`list_columns` 3–6) SHALL NOT change, and the
0.625 portrait factor SHALL NOT change.

#### Scenario: Four sizes differ on a wide viewport
- **Given** a 1920 dp window
- **When** the user picks each Card size in turn
- **Then** the rendered poster widths are Large 210 dp, Medium 180 dp, Compact 150 dp, Small 120 dp

#### Scenario: Large grows, Small stays smallest on a standard viewport
- **Given** a 960 dp window
- **When** the user picks Large, then Small
- **Then** Large renders 168 dp (larger than today's 144 dp) and Small renders 81 dp, clearly smaller than Compact's 99 dp

#### Scenario: Ceiling scale is bounded
- **Given** a 2560 dp window
- **When** the ceiling scale is computed
- **Then** it is 1.25, not 2.67, so Large renders 210 dp and never grows without bound

#### Scenario: Landscape artwork is untouched
- **Given** a landscape (16:9) card
- **When** its width is computed
- **Then** it is the landscape slot width, unchanged by the portrait policy

### Requirement: Theme gap used consistently in the grid

The shared results grid SHALL use the theme gap (`MatrixLayout.gap`) in
both its measurement and its arrangement, replacing the hardcoded 16 dp
in `horizontalArrangement`. On the default theme (16 dp gap) the layout
SHALL be unchanged.

#### Scenario: Non-default theme gap
- **Given** a theme with a gap other than 16 dp
- **When** the Search grid measures and lays out
- **Then** measurement and arrangement use the same gap value

### Requirement: Effective columns from one runtime-aware safe-mode source

Effective columns SHALL come from one function that takes the runtime
safe-mode flag: when the flag is on, the default (5) is in effect;
otherwise the saved `list_columns` value is. Home, the size settings
page, Search, and the list page SHALL all use it, passing
`repository.safeMode`. Intent-only safe mode (runtime flag on, persisted
pref off) SHALL therefore give the default columns on every screen.

#### Scenario: Intent-only safe mode
- **Given** safe mode is on only via the launch intent (persisted pref off)
- **When** Home and Search compute effective columns
- **Then** both use the default 5 columns

#### Scenario: Normal mode keeps the saved value
- **Given** safe mode is off and the saved value is 3 (Large)
- **When** Home and Search compute effective columns
- **Then** both use 3 columns

### Requirement: Image requests follow the rendered artwork width

Poster image requests SHALL be sized from the rendered artwork width,
so a larger poster decodes at the larger size. No decode path SHALL
assume the old 144 dp cap or any other fixed poster pixel size.

#### Scenario: Large poster decode size
- **Given** a Large poster rendering 210 dp wide on a 1920 dp window
- **When** its image is requested
- **Then** the request size is derived from the 210 dp rendered width
