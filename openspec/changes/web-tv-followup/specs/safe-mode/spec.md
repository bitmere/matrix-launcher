# safe-mode

## ADDED Requirements

- THE SYSTEM SHALL provide a safe mode that starts Home with
  wallpaper off, AI discovery off, and custom rows off (only app
  channels + favorites), with the default layout.

- SAFE MODE SHALL be on when any of these holds: (a) the settings
  toggle (`safe_mode` pref), (b) the launching intent carries the
  `safe_mode` boolean extra (per-launch, not persisted), (c) the
  crash counter reaches 3 at startup.

- THE CRASH COUNTER SHALL increment on every `onCreate` before
  `setContent` (written with `commit()`) and reset after the first
  successful composition; reaching 3 SHALL set `safe_mode` true and
  reset the counter, so safe mode is entered once and the user can
  exit via the toggle.

- WHEN safe mode is on, THE SYSTEM SHALL skip SIMKL sections/pins and
  custom lists in the Home load, return no wallpaper, and report AI
  discovery as disabled. Every gating point SHALL read the same flag.

- THE SYSTEM SHALL show a "Safe mode" status row on the Settings root
  page when active, with an "Exit safe mode" action, and the toggle
  on the same page.

- WHEN safe mode is off, THE NORMAL PATH SHALL be unchanged.

- THE FLAG/COUNTER transition (3 crashes → safe mode) SHALL be
  covered by a JVM-only unit test using a map-backed
  SharedPreferences fake, if a seam exists; otherwise it SHALL be
  verified by inspection and the gap stated in CHANGES.md.

## Non-goals

- No change to `MatrixHomeScreen.kt` unless unavoidable (see
  proposal.md). The gating lives in the activity, the repository,
  and the AI client.
- No automatic re-entry loop: entering safe mode resets the counter,
  and exiting via the toggle clears the flag.
