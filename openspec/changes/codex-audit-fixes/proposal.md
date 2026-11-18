# Codex audit fixes

## Why
The 2.7.0 audit identifies 31 defects and coverage gaps across portable settings,
credentials, updating, search, Home feeds, and lifecycle ownership. Prior partial
fixes must be checked against the current implementation, not assumed complete.

## What changes
- Validate and bound portable preference/theme input before writes. Keep credentials
  and recovery/device bookkeeping local; atomically replace validated theme files.
- Hide AI and TMDB keys on editor entry with a session-only MatrixIconButton eye toggle.
- Stabilize update row identity and retain verified-file retries. Correlate persisted
  installer sessions and operation callbacks, clean failed sessions, and own download
  cancellation through verification and return to the UI.
- Confine AI action aggregation, bound response/fan-out work, invalidate old search
  operations, search packages off Main, and resolve Unicode titles by media kind/year.
- Serialize SIMKL snapshot updates per type and shared activities/index refreshes;
  consistently propagate per-launch safe mode and retain the best channel candidates.
- Refresh theme geometry, release SAF ownership, tighten metadata parsing, remove
  write-only state, repair count-aware web caching and the structure guard.
- Add deterministic regression tests and record the separate device validation gate.

## Constraints
No new dependencies, permissions, services, version changes, changelog, workflow,
README, application-ID or preference-key/file renames. Keep the four legacy bridges,
existing safety policies and provenance. The audit and fix inputs remain unchanged.
UI additions use existing Matrix color/type/spacing/geometry tokens, TV Material 3,
and the shared MatrixIconButton; no new visual system or animation.

## Validation and limitations
Run focused JVM regressions, then the prescribed full JVM invocation and structure
fixtures. Do not bypass sandbox restrictions. No artifact installation or publication
is part of this change. Physical TV XML/D-pad, screenshot/glyph centering, installer
confirmation, Home/volume, account and AOT evidence must be recorded separately.
Existing backups made before this fix may contain TMDB credentials: treat them as
sensitive and rotate credentials if those backups were shared.
