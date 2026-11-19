## ADDED Requirements

### Requirement: AI search progress uses one quiet token-driven card with a Siri-style orb

The AI Discovery progress display SHALL be a single card built from
theme tokens (theme surface background, theme text, a single accent).
Its working indicator SHALL be a Siri-style orb: a 56dp luminous
sphere on a dark base, filled with a slowly rotating multicolour
swirl (blue, cyan, purple, pink, orange) around a bright white core,
with a faint rim, one slow rotation (one full turn per 6s, one
infinite animation), drawn on one Canvas in four layers (dark base,
rotating sweep-gradient swirl, bright core, thin rim) plus a soft
inner edge wash. The orb sits inside the card's header row next to
the sentence-case status line; it SHALL NOT take over the screen,
use a rainbow flow ribbon, gradient borders, or hard-coded colours
outside the orb. Its status labels SHALL be sentence case.

#### Scenario: AI search in progress
- **Given** an AI search is running
- **When** the progress display renders
- **Then** exactly one progress card is shown with a theme-surface
  background, a 56dp Siri orb whose swirl rotates once per 6s, a
  sentence-case status line, the current step text, and the last steps
  list — and no glow spill, ribbon, gradient border, or hard-coded
  colour is present outside the orb.

#### Scenario: No AI-slop remnants outside the orb
- **Given** the change is applied
- **When** `MatrixSearchScreen.kt` is searched for `Color(0x`
- **Then** zero matches are found, `MatrixSearchOrb.kt` exists with
  exactly one `rememberInfiniteTransition`, and the same token
  treatment covers the AI summary box (theme surface, 1dp
  `onSurface` border, primary dot and label), the voice listening
  glow (single theme accent, no hard-coded palette), and the
  "Discovering live trends…" line (primary).

#### Scenario: Orb motion is bounded and calm
- **Given** the Siri orb
- **When** its rotation is measured over time
- **Then** it completes exactly one turn per 6s, the swirl palette
  holds five distinct Siri hues in sweep order with the first colour
  repeated at the end for a seamless wrap, and no second animation
  (scale, pulse, glow, colour shift) runs alongside it.

#### Scenario: Swirl and glow are contained inside the orb
- **Given** the Siri orb
- **When** its draw bounds are checked
- **Then** every layer is clipped to the orb circle, the glow radius
  fraction is at most 1.0 (nothing drawn beyond the orb radius),
  the bright core radius is a fraction of the orb radius, and no
  colour or glow reaches the card outside the orb.

### Requirement: Siri orb is a deliberate, recorded override of the earlier ring

The earlier rebuild (2026-11-19 21:39 IST) made the orb a 28dp
token-driven ring under the no-ai-slop guidelines. The user
redirected (2026-11-19 21:51 IST: "it should be like apple siri
orb"), overriding the ring for the orb itself. The multicolour
gradient SHALL be allowed inside the orb only, because the user
asked for the Siri look; the no-ai-slop banned list otherwise
remains in force everywhere else. The Siri swirl colours SHALL be
named constants in `MatrixSearchOrb.kt`, documented as Siri
identity colours rather than theme tokens, so a custom theme does
not recolour them.

#### Scenario: Override is recorded, not silent
- **Given** the Siri direction
- **When** the proposal, this spec, the tasks, and the orb file
  header are read
- **Then** each records the 21:51 IST redirect, the inside-the-orb-only
  scope of the override, and that the Siri colours are identity
  colours, not theme tokens.

### Requirement: Progress presentation changes nothing else

The search behaviour SHALL be unchanged: the same query runs, the
same results render, and the same step stream is produced. Only the
progress presentation changes.

#### Scenario: Same search, same results
- **Given** an AI search that previously completed
- **When** the same search runs after this change
- **Then** the same results are shown, with the same step texts,
  on the quiet progress card.
