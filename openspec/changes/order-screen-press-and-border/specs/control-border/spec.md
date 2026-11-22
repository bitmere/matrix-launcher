## ADDED Requirements

### Requirement: Unfocused settings rows have no border

An ordinary settings row SHALL have no border when unfocused. The
focused row SHALL keep its 1.5 dp solid `onSurface` border. The
destructive unfocused border SHALL stay as-is.

#### Scenario: Settings page at rest
- **Given** a settings page with no row focused
- **When** it renders
- **Then** no row shows a pale border; rows are separated by spacing and
  fill only

#### Scenario: Focused settings row
- **Given** a settings row has D-pad focus
- **When** it renders
- **Then** the 1.5 dp focused border is visible

### Requirement: No other border or glow changes

The home-card borders, the focused-card glow, the text-field surface
border, and the poster letterbox background SHALL be unchanged by this
change.

#### Scenario: Home cards after the change
- **Given** the Home screen
- **When** cards render unfocused and focused
- **Then** the unfocused border is still none and the focused glow and
  outline are unchanged
