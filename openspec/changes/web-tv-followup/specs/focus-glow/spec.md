# focus-glow

## MODIFIED Requirements

- WHEN a card gains focus, THE SYSTEM SHALL animate its glass-rim
  rotation once — a single 0→360 sweep over ~1.5 s — and then hold
  the angle still, instead of revolving forever on a 4 s loop.

- WHEN focus leaves a card, THE SYSTEM SHALL snap the rotation back
  to 0, as today.

- WHEN Home is at rest with a card focused, THE SYSTEM SHALL draw 0
  frames: no animation state changes after the single sweep settles.

## Non-goals

- No change to the focus outline alpha (0→1, holds at 1 while
  focused), to `cardFocusScale = 1.015f`, or to the backdrop palette
  change. The card stays clearly highlighted; only the motion stops.
- No change to the search-screen animations (orb, voice pulse, AI
  progress) — they run only while searching/listening, not on Home.
