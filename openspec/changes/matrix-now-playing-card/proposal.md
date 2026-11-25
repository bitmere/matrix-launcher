# Proposal — matrix-now-playing-card

## Why
Home has no current-session playback controls. The approved 2026-11-25 design
adds one wide, blue-tinted glass card above Favorites, without turning Home into
a control centre or mistaking published Watch Next rows for active playback.

## Scope
- Observe platform media sessions through the existing approved notification
  listener; preserve its legacy component and launcher-revival behavior.
- Use an Android-free session-store seam, worker-thread adapter and in-memory
  state. Prefer playing, then paused, then platform priority; hide all other states.
- Add a pinned MediaRow through HomeSnapshot, with title, session app label,
  optional bounded artwork, play/pause and advertised previous/next controls.
- Add a default-shown `matrix:now-playing` identity in HIDDEN_ROWS, controlled by
  rowShown from Settings > Home channels even when no session exists.
- Use MatrixGlassBorder colour glow, shared control geometry and MatrixIconButton;
  frost only this card's backdrop. Android 12+ uses blur; older/cheap devices use
  translucent material. Preserve all global focus borders.
- Add plain JVM selection, visibility, action and stale-command tests.

Sources: [active-session access](https://developer.android.com/reference/android/media/session/MediaSessionManager),
[permission protection level](https://android.googlesource.com/platform/frameworks/base/+/main/core/res/AndroidManifest.xml),
[listener lifecycle and low-RAM limits](https://developer.android.com/reference/android/service/notification/NotificationListenerService),
[metadata](https://developer.android.com/reference/android/media/MediaMetadata),
[actions](https://developer.android.com/reference/android/media/session/PlaybackState),
[transport controls](https://developer.android.com/reference/android/media/session/MediaController.TransportControls),
[blur support](https://developer.android.com/reference/kotlin/androidx/compose/ui/draw/package-summary).
Investigation was written before implementation to `/tmp/matrix-now-playing-findings.md`.

## Non-goals
No progress bar, new permission declaration, notification-content parsing,
playback history, per-app scraping, dependency, version bump, global border fix,
or launcher-wide glass. No claim of universal app support: YouTube, Stremio and
Nuvio require device validation. Live-TV apps can expose sessions, but reliable
current channel names need a separate integration; v1 displays the app name.
No builds, test/lint runs, commit, push, PR, install or release in this round.
