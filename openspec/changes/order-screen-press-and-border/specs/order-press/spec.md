## ADDED Requirements

### Requirement: A short OK press on an order row acts immediately

A short OK press on a channel order row SHALL pick the row up to move
at once, with no timing gate after the page opens. The first press after
opening the page SHALL NOT be dropped.

#### Scenario: Fast OK after opening the order page
- **Given** the channels order page has just opened
- **When** the user presses OK on a row within 400 ms of opening
- **Then** the row is picked up to move and the "Moving" state shows

### Requirement: Holding OK toggles row visibility without key repeats

Holding OK on a channel order row for ~380 ms SHALL toggle that row's
visibility on Home, on remotes that send no key-repeat events while a
key is held. Releasing before the hold time SHALL act as a short press
instead, and SHALL NOT also toggle visibility. While a row is being
moved, holding OK SHALL NOT toggle visibility.

#### Scenario: Hold OK on a remote with no key repeats
- **Given** a channel row on the order page and a remote that sends only
  one DOWN and one UP for a held OK
- **When** the user holds OK for ~380 ms or longer
- **Then** the row's visibility toggles and the row updates at once

#### Scenario: Short press after a hold is suppressed
- **Given** a hold-OK visibility toggle has just fired
- **When** the user releases OK
- **Then** no move begins

### Requirement: Order rows show their visibility state

Each order row where visibility applies SHALL show a visible ✓ when the
row is shown on Home and ○ when hidden, in addition to the existing
"Shown on Home"/"Hidden on Home" detail text. Rows where visibility
does not apply SHALL show no mark.

#### Scenario: Hidden row
- **Given** a channel row that is hidden from Home
- **When** the order page renders
- **Then** the row shows ○ and the detail reads "Hidden on Home"

### Requirement: An UP without its DOWN never acts

An OK release (UP) on a row SHALL act only if that same row saw the
matching press (DOWN). An UP whose DOWN went to another row — focus
moved mid-hold — SHALL be swallowed, never starting a move or a toggle
on the new row. Losing focus SHALL end the press gesture for that row.

#### Scenario: Hold OK, move focus, release
- **Given** OK is held on row A past the hold time and focus moves to row B
- **When** OK is released
- **Then** row B does not start moving and row A does not toggle again

Pressing the gamepad A button while a row is being moved SHALL save the
move, the same as DPAD_CENTER / Enter / Numpad Enter.

#### Scenario: Save a move with gamepad A
- **Given** a row is being moved on the order page
- **When** the user presses the gamepad A button
- **Then** the move is saved and "Order saved" shows
