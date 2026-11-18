# Tasks — perf-refresh

Work in order. Each task names its exact files and how to verify it. Do not
start the next task until the current one verifies. No builds run here —
verification is grep, code inspection and (where a test exists) the test
itself; the main agent compiles and runs CI after the diff review.

1. D-01 — Delete dead `fetchCinemetaCatalog`.
   Files: `app/src/main/java/com/matrix/launcher/MatrixSearchRepository.kt:524`.
   Verify: grep finds zero callers in `app/src` and `core/src` (main + test).

2. D-04 — Hoist per-call regexes to `private val` constants.
   Files: `MatrixSearchRepository.kt:153,452,464,489,504,509-511`,
   `MatrixWebSearchEngine.kt:177-206`, `CommonsWallpapers.kt:33,35`.
   Verify: grep finds no `Regex(` construction left in those functions.

3. D-05 — Bound the web-search cache and cap reads.
   Files: `MatrixWebSearchEngine.kt:29,55-75,97,148`.
   Change: cache becomes an LRU of 32 entries that evicts on write;
   DuckDuckGo and Wikipedia reads capped at 512 KB; the two fetches run
   in parallel with `async` instead of sequentially.
   Verify: code inspection that the map is bounded and both reads are
   capped; add a unit test for eviction if a test file for the engine
   already exists.

4. D-02 — Dedupe and memoize AI resolution.
   Files: `MatrixSearchRepository.kt:195-297,300-437`,
   `CinemetaClient.kt:18,54`.
   Change: dedupe recommendations by normalized title before resolving;
   memoize `getMeta` / `searchCatalog` results within one `searchAi`
   call in a small `LinkedHashMap` (max 64 entries).
   Verify: result set is unchanged (same titles, same order) — only the
   number of network calls drops; existing tests stay green.

5. D-03 — Split the lock; parallelize the Home feed.
   Files: `ComposeHomeRepository.kt:22,48,80,108`,
   `MatrixSearchRepository.kt:164,224`,
   `MatrixAccountsRepository.kt:15-28`, `HomeFeedSource.java:6-28`.
   Change: `gate` becomes a home lock and a SIMKL lock; the SIMKL search
   in `searchAi` leaves the home lock; `HomeFeedSource.append` fetches
   SIMKL sections and pins in parallel. Lock order stays acyclic
   (home → SIMKL → client monitor). Touch D-07 only if the split
   requires it.
   Verify: code review that no path acquires the locks in the opposite
   order; existing tests stay green.

6. Search force-refresh.
   Files: `MatrixSearchRepository.kt` (`search()` gains
   `forceRefresh: Boolean = false`; when true it skips the 10-minute
   cache read for that key and overwrites the entry),
   `MatrixWebSearchEngine.kt` (matching bypass, threaded through the
   AI path's web lookups), `MatrixSearchScreen.kt` (`submit()` gains
   the flag; a Refresh `MatrixIconButton` using
   `R.drawable.ic_home_refresh` sits in the header Row after the
   Submit button, shown only when the query is non-blank, and calls
   `submit(forceRefresh = true)`), `strings.xml` (one English string
   for the label, `search_` prefix).
   Do not touch the 30-minute suggestions cache — refresh is about
   results, not suggestions.
   Verify: JVM-only unit test proving a second search with
   `forceRefresh = true` re-fetches while a second search without it
   returns the cached result. If the existing tests hit real network,
   do not add a network test — verify by inspection and say so.

8. D-07 — Narrow SIMKL's locking (prerequisite for task 9).
   Files: `SimklClient.java` (`access()`, `request()`, `search`,
   `index`, `list`, `library`, `imdb`).
   Change: the monitor guards only token state (refresh + save),
   quota-policy state (blocked check, observe, savePolicy) and the
   cache-file state (`cachePrepared`, file writes). Network I/O — the
   HTTP call in `request()` and the token-refresh call in `access()` —
   happens outside the monitor. Token refresh: at most one refresh in
   flight; concurrent callers wait for it and then re-read the fresh
   token (never refresh twice, never use a token obtained before a
   refresh that superseded it). Document the lock order (simklGate →
   token/policy lock, never the reverse).
   Verify: existing tests stay green; same responses, same errors, same
   quota behaviour — only the scheduling changes. Code review that no
   path holds the monitor across network I/O.

9. D-13 — Fetch the Home feed sections and pins in parallel.
   Files: `HomeFeedSource.java` (`append`), `SimklClient.java` if the
   fetch shape needs it.
   Change: sections (in `LIBRARY` order) and pins (sorted) are fetched
   concurrently once task 8 allows it; results are assembled in the
   same order as today (sections first, then pins, inserted after the
   Nuvio rows); a failing section or pin is still isolated to the
   "Some lists could not be refreshed" status and never fails the
   whole feed.
   Verify: same rows, same order, same error behaviour — only faster;
   existing tests stay green.

10. Unit tests for the two gaps.
    Files: `MatrixSearchRepository.kt` (minimal internal test seam),
    `HomeFeedSource.java` (if a seam is needed for the assembly test),
    `MatrixSearchTest.kt` (and a feed test file if one fits better).
    Change: (a) force-refresh bypass test — with the 10-minute cache
    seeded, a second search without forceRefresh returns the cached
    cards, and with `forceRefresh = true` it does not (it re-fetches);
    the seam is internal and test-only (e.g. a `@VisibleForTesting`
    cache seed plus a testable cache-read decision), with no production
    behaviour change when `forceRefresh` is false. (b) parallel
    assembly test — sections and pins arrive out of order and one
    section fails, yet the assembled rows are in the same order as
    today and the failure is isolated.
    Verify: both tests are JVM-only (no network) and pass; if a seam
    genuinely cannot be added without a bigger refactor, verify by
    inspection and say so in CHANGES.md — do not pretend a test
    exists.

11. Final verification (main agent, after the user reviews the diff):
   `:app:compileDebugKotlin`, `testDebugUnitTest`, `lint`, then PR
   and CI. Nothing is pushed or compiled before the diff review.
