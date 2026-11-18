# Feedback fixes (2.5.3 follow-up)

## Why

User feedback on the installed 2.5.3 build, reported 2026-11-14:

1. The focus glow is too fast to see: one 1.5 s sweep, then it holds still. The user
   asked for ten to fifteen seconds of visible glow, then stop.
2. The Theme page's token rows cannot be seen or changed: the ten rows are
   display-only status rows above the Apply button, focus lands on Apply, and
   D-pad Up has no focusable row above it, so the rows are unreachable. The user
   also asked why they are read-only and how Apply works.
3. Cinemeta search posters load very slowly. The poster files are small (Cinemeta
   serves a "small" poster, TMDB posters are w342), so the wait is the per-title
   lookup chain before the poster URL is known. One redundant round-trip is
   removable: when the Cinemeta catalog already matched a title and supplied a
   poster, the code still fetches the title's meta a second time when only the
   IMDb rating is missing.

## Scope

1. Focus glow: sweep for 12 s (three 4 s revolutions) when focus lands, then hold
   still. No change to the focus outline, the 1.015 scale, or the backdrop.
2. Theme page: the ten token rows become tappable rows that cycle their value
   (focus zoom, tile size, corner radius, gap, and the six colours). A tap saves
   the new value to theme.json, applies it, and restarts Home — the same
   mechanism the Apply button already uses. Apply / Import / Reset keep their
   current meaning. The page description explains tapping a row to change it.
3. Search posters: in `resolveCinemeta`, the second meta fetch runs only when the
   poster or the year is missing, not when only the IMDb rating is missing. The
   SIMKL cross-reference still runs and can still supply a rating.

## Non-goals

- No change to how channels / Continue Watching / Now Playing are read (that
  feedback was answered: apps appear only if they publish an Android TV channel
  and are selected in Settings → Home channels).
- No parallelisation of the per-title lookup chain and no streaming of search
  results (both are bigger changes; revisit if posters are still slow after this).
- No colour picker UI; colours cycle through a short preset list.
- No new dependencies, permissions, background services, or preference keys.

## Constitution

Repo AGENTS.md: keep the Apple-style Home, shared Matrix roles, TV Material 3,
deterministic D-pad behaviour; `:core` stays Android-free; no new DI framework;
collect edits before building; version from version.properties.
