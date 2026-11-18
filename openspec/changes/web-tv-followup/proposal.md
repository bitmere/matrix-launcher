# Change: web-tv-followup — Web TV Launcher lessons for Matrix

One change, one branch, one PR. It applies the usable ideas from the
Web TV Launcher comparison (dev.to, 2026-11-12): stop Home drawing at
rest, add a bench harness so performance claims are measured on the
user's TV, add level-0 theme tokens, add settings backup export/import,
and add a safe-mode fallback Home. It also fixes the stale claims in
the custom Android skills and adds the two missing skills (bench and
TV smoke test).

## Why

- Home never stops drawing at rest: the focused card's glass rim
  revolves forever on a 4 s loop, and on a TV something is always
  focused. That is the one clear idle-frames violation in the launcher.
- Performance items D-06 (row cache windows) and D-22 (prefs-revision
  recomposition) were deferred as "measure first" — but there is no
  bench harness to measure them with, so they can never be settled.
- The user already lost all settings once (uninstall for the signing-key
  change) and there is still no backup export.
- A bad build can brick the home screen with no recovery except
  uninstalling (which loses settings again).
- The three custom Android skills still claim this VM has no Java,
  Gradle, or Android SDK — false since the toolchain was installed
  2026-11-14 — and the testing skill's counts and recommendations are
  outdated.

## Scope

1. Focus glow: the rotation animates once when focus lands and then
   holds still. The card stays clearly highlighted (border + 1.015
   scale + backdrop); only the motion stops.
2. Bench harness (`tools/bench.py`) + `tv-bench` skill: a scripted
   D-pad tour over adb with frame/memory/cold-start numbers, 3-run
   medians against the current release baseline, and gates.
3. Theme tokens: a `theme.json` file (background, panel, surface, text,
   accent, radius, gap, focusScale, tileSize) loaded at startup into a
   ThemeState, plus a Theme page in the settings registry. Validated
   with bounds and contrast checks; bad files fall back to defaults.
4. Settings backup export/import as versioned JSON via the Storage
   Access Framework (no new permissions). Secrets are never exported.
5. Safe mode: a flag (settings toggle, adb intent extra, or 3
   consecutive startup crashes) that starts Home with wallpaper, AI
   discovery, and custom rows disabled and the default layout.
6. Measurement procedure for the focus zoom (1.015 vs 1.0) and the
   backdrop crossfade — documented in the tv-bench skill, no code
   change.
7. Docs and skill hygiene: fix the stale claims in the three existing
   custom skills, add `tv-smoke-test` skill, add `docs/tv-gotchas.md`
   and the "numbers first" workflow note.

## Non-goals

- No WebView anywhere, no CSS, no theme gallery, no LAN install or QR
  install. The skin idea is done natively (theme tokens); the rendering
  engine stays native Compose + TV Material 3.
- No change to the focus outline alpha, the 1.015 focus scale default,
  or the backdrop behaviour — item 1 stops the motion only.
- No change to `MatrixHomeScreen.kt` unless the implementer finds it
  unavoidable; the AOT plain-text layout lives there and is on the
  unmerged perf-refresh branch, so touching it creates a rebase
  conflict. If it must be touched, base is main's version and the
  rebase note below applies.
- No new dependencies, permissions, background services, or DI
  frameworks. The backup uses the Storage Access Framework, which
  needs no permission.
- No changes to PR #2 or the perf-refresh branch.

## Constitution

The repo's `AGENTS.md` is the project constitution: optimized, modular
code that runs with a small footprint on low-memory TVs; no
functionality lost; TV Material 3; deterministic D-pad behaviour; no
new dependencies or permissions. The user reviews the diff before
anything is compiled.

## Base and rebase

Base is main @ `84f9d57`. The checkout differs from main in three
files the perf-refresh change touched: `strings.xml` (one extra line,
`search_refresh_results`), `ComposeHomeRepository.kt` (the lock split),
`MatrixAiClient.kt` (forceRefresh threading). This change's draft is
built on main's versions of those files. If PR #2 merges before this
change is pushed, the branch must be rebased onto the new main: keep
`search_refresh_results` in `strings.xml`, and keep the lock split and
the forceRefresh threading when re-applying this change's edits to
`ComposeHomeRepository.kt` and `MatrixAiClient.kt`. Do not rebase
anything now.

## Open questions

- None. The focus-glow fix, the theme token list, and the safe-mode
  triggers were all approved by the user in the comparison discussion
  (2026-11-14).
