# Change: perf-refresh — P0 performance fixes + search force-refresh

One change, one branch, one PR, released as 2.5.3. It combines the five P0
findings from the dead-code & optimization diagnosis (D-01–D-05) with the
search force-refresh feature, because they touch the same search and
repository code and splitting them would mean two overlapping diffs.

## Why

- One AI search can fire ~100 TMDB searches plus ~200–300 Cinemeta calls,
  with no dedup or caching of repeated titles.
- One shared `gate` Mutex serializes Home load, SIMKL work and media
  launching, so a slow SIMKL call stalls Home itself.
- Regexes are compiled fresh on every call on hot paths (thousands of
  compilations per AI search).
- The web-search cache grows for the life of the process and never evicts.
- A repeated search returns cached results for 10 minutes (web: 1 hour)
  with no way to force fresh results.

## Scope

1. D-01: remove dead `fetchCinemetaCatalog` (zero callers).
2. D-02: dedupe AI recommendations and memoize lookups within one search.
3. D-03: split the single lock into a home lock and a SIMKL lock; load the
   Home feed sections in parallel.
4. D-04: hoist per-call regexes to constants.
5. D-05: bound the web-search cache, cap reads, fetch sources in parallel.
6. Search force-refresh: a Refresh button that re-runs the current query
   bypassing the caches.
7. AOT info layout (plain text, title + fully wrapped subheader, no icon,
   no container) — already implemented and compiled in the checkout
   (MatrixMenu.kt, MatrixSettingsComponents.kt, MatrixHomeScreen.kt);
   it joins this release branch at assembly time, not through this
   draft, so the draft must not touch those three files.
8. D-13 + D-07 (follow-up, user-approved 2026-11-14): narrow SIMKL's
   locking so network I/O leaves the monitor, then fetch the Home feed
   sections and pins in parallel. The main hazard is token-refresh
   races; at most one refresh may be in flight.
9. Unit tests (follow-up): a JVM-only test for the force-refresh bypass
   (via a minimal internal seam) and for the parallel feed assembly
   (order + per-section error isolation).

## Non-goals

- P1 items that need measuring on the TV first (row cache windows D-06,
  prefs-revision recomposition D-22).
- Capping how many AI recommendations get resolved (D-02c) — measure the
  dedupe + memoize win first; capping changes visible results.
- Any visible behaviour change, except the AOT info layout (item 7
  above, already approved by the user). Same results, same order, same
  screens otherwise.
- The AOT layout work itself — it is done; see item 7.
- New dependencies, permissions, background services, or DI frameworks.

## Constitution

The repo's `AGENTS.md` is the project constitution: optimized, modular code
that runs with a small footprint on low-memory TVs; no functionality lost;
TV Material 3; deterministic D-pad behaviour; no new dependencies or
permissions. The user reviews the diff before anything is compiled.

## Open questions

- Resolved 2026-11-14: D-13 needs D-07, so D-07 is now in scope
  (tasks 8–9). The D-03 split itself did not need it; the parallel
  Home feed does.
