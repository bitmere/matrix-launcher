# Impeccable P1 top 4 — Matrix 2.7.7

Fix the first four P1 findings from the read-only Impeccable critique of
Matrix 2.7.7 (`~/workspace/github-audit/matrix-impeccable-critique/report.md`).
Nothing else: not P1-5, P1-6, not any P2.

1. **AI search progress is the one AI-slop cluster in the app (P1-4).**
   The Search → AI Discovery progress screen is a different product's
   visual language: a 150dp glowing orb with 8 draw layers and 3 infinite
   animations (`MatrixSearchOrb.kt`), a "Real-Time AI Streaming Console
   Card" with a 7-colour rainbow flow ribbon, cyan/purple gradient
   borders, hard-coded `0xFF00F0FF` / `0xFF38BDF8` throughout
   (`MatrixSearchScreen.kt:777-880`), and all-caps eyebrow labels
   ("LIVE AI REASONING STREAM", "AI DISCOVERY & SYNTHESIS REPORT").
   None of it uses theme tokens, so a custom theme cannot tame it, and it
   hits the no-ai-slop banned list and the craft-floor kicker ban.
   Replace it with one quiet token-driven progress card and rebuild the
   orb properly inside it: a small token-driven ring (see the ai-search
   spec), not a deletion — the user asked for the orb built properly
   under the new guidelines, not called slop and removed.
   Same search, same results — only the progress presentation changes.
   **Redirect 2026-11-19 21:51 IST: "it should be like apple siri
   orb".** The user overrode the ring for the orb itself: the orb is
   now a 56dp Siri-style luminous orb — a dark base with a slowly
   rotating multicolour swirl (blue, cyan, purple, pink, orange)
   around a bright white core, one slow rotation per 6s, everything
   clipped to the orb circle. The multicolour gradient is allowed
   INSIDE the orb only, as a deliberate override recorded in the
   ai-search spec; the banned list still governs everything outside
   the orb, and the card, status line, and last-4 steps stay
   token-quiet.

2. **Every theme tap restarts the whole activity (P1-1).** Settings →
   Theme calls `scope.recreate()` per tap (`MatrixSettingsPages.kt`,
   `applyEdited()` / `applyLoaded()` / reset), so cycling 4–6 presets
   means 4–6 full Home restarts with a visible flash, and focus drops
   back to the first favorite each time. Yet `MatrixLayout.theme` is
   already observable `mutableStateOf` and `MatrixTvTheme` reads it in
   composition, so colours/radius/gap/scale can apply live; only
   `tileSize` needs a data refresh, which the Size page already does
   with `refresh(false)` and no recreate. Apply theme state live in
   place; recreate only if a value genuinely cannot apply live.

3. **A Cinemeta/network outage is reported as "no results" (P1-2).**
   `CinemetaClient.searchCatalog()` swallows every failure into
   `emptyList()` (`getOrDefault(emptyList())`);
   `MatrixSearchRepository.cinemetaSearch()` propagates no error; the
   screen then shows `No movies or series matching "$query" found`.
   A user on flaky TV Wi-Fi is told the title does not exist. Return a
   Result/sealed outcome and show honest copy ("Catalog unavailable —
   check connection") when the catalog calls fail. Successful-search
   behaviour does not change.

4. **Focus + poster states (P1-3 and P1-7, one pair).** Focused cards
   are distinguishable only by a tinted glow and 1.015× scale
   (`MatrixCardDefaults` sets border None everywhere); the glow is
   sampled from the artwork palette, so on a bright poster it can be
   near-invisible, and the title text does not change on focus.
   And `MatrixMediaCard`'s `AsyncImage` has no placeholder/error, so
   first load is grey boxes and a failed poster is the same grey box —
   loading and broken are visually identical. Add a theme-driven solid
   focus outline plus a brightened/bold focused title, and a
   token-driven placeholder plus a distinct error state for posters.

## Constraints

- Tokens first (`MatrixTheme.kt`, `theme.json`); work inside the tokens
  recorded in `DESIGN.md`. No new palette, no new typeface.
  **One deliberate exception, per the user's explicit direction
  (2026-11-19 21:51 IST): the Siri swirl colours inside the orb are
  Siri identity colours, not theme tokens, and a custom theme must
  not recolour them. The exception covers the orb interior only.**
- The no-ai-slop-design skill governs the banned list; run the
  Impeccable review passes on these UI changes and the detector before
  calling the work done.
- D-pad-first 10-foot TV UI; must work on old and new TVs and low-RAM
  devices. Lose no functionality. Behaviour changes are limited to what
  the four findings require.
- The safe-mode `recreate()` calls in `MatrixSettingsPages.kt` are out
  of scope and stay as they are.
- Matrix has no `androidTest` directory — unit tests only. Anything
  needing a real TV (focus obviousness at 10 feet on bright posters,
  poster first-load time on TV Wi-Fi, live theme apply on device) is
  recorded as NEEDS-TV, untested until the user confirms on the TV.
