# bench

## ADDED Requirements

- THE SYSTEM SHALL provide a bench script (`tools/bench.py`, Python 3,
  stdlib only) that runs a fixed scripted D-pad tour over adb: wake
  the TV, cold start with `am start -W`, 20 focus moves, 6 row
  changes, menu open/close, Home press, and a 30 s idle window.

- THE SCRIPT SHALL collect `dumpsys gfxinfo <package> framestats`
  (janky frames, p95 total frame time, idle frame rate),
  `dumpsys meminfo <package>` (Total PSS), and cold start TotalTime.

- THE SCRIPT SHALL run 3 runs by default, report per-run and median
  values, and compare medians against a stored baseline JSON file.

- THE SCRIPT SHALL report pass/fail per gate: ≤1.5 janky frames per
  key, p95 frame ≤1.15× the reference in the same session, key-latency
  p95 <50 ms (when the instrumentation exists; otherwise "not
  measured"), ≤1 idle frame/s, PSS <182 MB, cold first frame ≤1.15×
  reference — and SHALL always print the raw numbers alongside the
  verdicts.

- THE SCRIPT SHALL measure release builds only and drop the first
  cold-start run.

- WHEN key latency cannot be measured from outside the app, THE
  SYSTEM SHALL provide a debug-flagged in-app recorder (off by
  default, inert when off) hung off the existing inspection hook and
  exposed through the activity's existing `dump()` override.

## Non-goals

- The bench script is not run on this VM — the user's TV is not
  reachable from here. It is delivered for the user to run.
- No new permissions, no background service, no always-on
  instrumentation. The recorder is debug-only and off by default.
