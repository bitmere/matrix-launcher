# Home Now Playing

## ADDED Requirements

### Requirement: Current-session row
Home SHALL prepend one wide Now Playing row above Favorites using HomeSnapshot's
row model. It SHALL show only an active session in PLAYING or PAUSED state, with
playing preferred over paused and platform priority breaking ties. Stopped,
absent, inaccessible and other states SHALL produce no card. Playback state
SHALL remain in memory only.

#### Scenario: Playback changes or ends
- GIVEN notification access and an eligible session
- WHEN metadata, state, actions or active sessions change
- THEN the row updates without reloading content feeds
- AND stop, session destruction or loss of access removes obsolete content.

### Requirement: Session controls and content
The row SHALL show title and application label from the session package. It
SHALL prefer display artwork, then media/album artwork, then a neutral icon.
There SHALL be no progress bar. Only publisher-advertised actions SHALL be
available; dispatch SHALL recheck selected identity, state and action support.

#### Scenario: Unsupported skip or stale click
- GIVEN a session without skip support, or an action from a replaced session
- WHEN the row renders or the old action is requested
- THEN unsupported skip controls are absent and stale commands are rejected.

#### Scenario: Live TV
- GIVEN a live-TV app exposing a compatible session
- THEN v1 shows its session title and app name
- AND SHALL NOT infer a currently tuned channel from Home channel catalogues,
  artist fields or Watch Next history.

### Requirement: Optional access and visibility
The existing legacy GuardRevival listener SHALL retain its component identity
and revival responsibilities. Media observation SHALL begin after connection
and unlock, clean up on disconnect/destruction, and run framework I/O off main.
No new manifest permission SHALL be added. An existing notification access
approval is sufficient; otherwise the user must enable Matrix in Settings >
Apps > Special app access > Notification access, or use
`adb shell cmd notification allow_listener com.matrix.launcher/com.gathin.tvhome.GuardRevival`.
Code alone SHALL NOT be treated as proof of the user's TV approval.

#### Scenario: Hide while idle
- GIVEN no current session
- WHEN the user hides Now playing in Home channels settings
- THEN rowShown records `matrix:now-playing` in HIDDEN_ROWS
- AND future eligible sessions stay hidden until shown again; default is shown.

### Requirement: Scoped material and D-pad
The new card SHALL use rounded, translucent blue glass with a hairline rim and
MatrixGlassBorder colour glow on focus. Only its background SHALL be blurred on
supported devices; older/cheap devices SHALL retain a lightweight translucent
fallback. Text and artwork SHALL stay sharp. Shared focusedCardBorder and
onSurface focus-border definitions SHALL remain unchanged.

#### Scenario: Remote interaction
- GIVEN the row is present
- WHEN OK is pressed on its focusable content
- THEN the supported play/pause action is sent; unsupported playback has no action
- AND separate icon controls use MatrixIconButton and shared geometry
- AND horizontal navigation stays within the row, with vertical exits available
- AND a removed focused control returns focus to the card; a disappearing focused
  row returns focus to Home's existing initial target.
