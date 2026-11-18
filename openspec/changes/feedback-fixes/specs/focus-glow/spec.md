# focus-glow

## ADDED Requirements

### Requirement: Focus glow stays visible for twelve seconds, then stops
When focus lands on a card, the glow SHALL revolve for 12 s (three 4 s
revolutions) and then hold still at its final angle, so the glow is visible
long enough to be seen and Home draws 0 frames at rest afterwards.

#### Scenario: Focus lands on a card
- **WHEN** a card gains focus
- **THEN** the glow angle animates from 0 to 1080 over 12 s with linear easing
- **AND** after the animation the angle holds at 1080 (visually identical to 0
  for the periodic glow shader) with no further frames

#### Scenario: Focus leaves mid-sweep
- **WHEN** focus leaves the card before the 12 s elapse
- **THEN** the rotation snaps back to 0 and no animation continues

### Requirement: Highlight other than the glow is unchanged
The focus outline (160 ms fade, holds at 1), the focus scale token, and the
backdrop change SHALL behave exactly as before this change.
