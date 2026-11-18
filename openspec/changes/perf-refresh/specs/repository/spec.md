# Repository capability — delta spec

## MODIFIED Requirements

- Home load, SIMKL operations and media launching SHALL NOT serialize
  behind a single lock. Home work and SIMKL work SHALL use separate
  locks. (D-03)

- Lock acquisition order SHALL remain acyclic: home lock, then SIMKL
  lock, then the client monitor — never the reverse. (D-03)

- WHEN the Home feed loads, THE SYSTEM SHALL fetch its SIMKL sections
  and pins in parallel rather than strictly one after another. (D-13)

- SIMKL network I/O SHALL NOT happen while holding the client's
  monitor. The monitor guards only token state, quota-policy state
  and cache-file state. (D-07)

- Token refresh SHALL have at most one refresh in flight. Concurrent
  callers SHALL wait for it and then re-read the fresh token; the
  system SHALL never refresh twice for the same expiry and never use
  a token obtained before a refresh that superseded it. (D-07)

- WHEN the Home feed assembles parallel results, THE SYSTEM SHALL keep
  the same rows in the same order (sections in LIBRARY order, then
  pins sorted, inserted after the Nuvio rows), and a failing section
  or pin SHALL be isolated to the "Some lists could not be refreshed"
  status without failing the whole feed. (D-13)

## REMOVED Requirements

- `fetchCinemetaCatalog` SHALL have no reference anywhere in the code:
  it has zero callers, and production search goes through
  `cinemetaSearch()` instead. (D-01)

## Non-goals

- D-07 (dropping `synchronized` in `SimklClient`) is now IN scope
  (tasks 8–9) because the parallel Home feed needs it.
- No change to what Home shows, to SIMKL behaviour, or to account
  sign-in flows — only to how the work is scheduled.
