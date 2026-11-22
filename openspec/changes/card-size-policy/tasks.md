# Tasks — Shared card-size policy

- [ ] Shared preferred width: in `MatrixLayout`, add one pure
  preferred-portrait-width calculation used by Home rows, the Search grid,
  and the list page. It takes the measured window width and the
  `list_columns` value (3–6) and returns the poster frame width:
  `min(cardWidth(windowWidth, columns, channelInset) × 0.625,
  portraitCeiling(columns, windowWidth))`. The ceiling scale uses the
  full window width on both screens — the Search screen must pass its
  window width through the panel to the grid (do not scale the ceiling
  by the narrower panel width, or the screens disagree again). Each
  container then fits that width into its own layout
  (`fittingColumns` / `rowFit` / `MatrixFocusRow`); the existing
  `coerceAtMost(slotWidth)` backstop stays.
- [ ] Four-size portrait policy: replace the single 144 dp ceiling in
  `MatrixLayout.mediaWidth` with per-size ceilings — base ceilings at
  the 960 dp reference window Large 168 / Medium 144 / Compact 120 /
  Small 96 dp, scaled by `(windowWidth / 960).coerceIn(0.75, 1.25)`.
  Keep the same pref key/values (`list_columns` 3–6) and the 0.625
  portrait factor. Remove the old 144 dp constant; no other caller may
  keep a private copy of it. The non-portrait path of `mediaWidth`
  (returns the landscape width unchanged) stays.
- [ ] Callers: Home channel rows (`MatrixHomeSurface.kt`, `preferred`
  and the mixed-row `artworkWidth`) and the shared grid
  (`MatrixMediaGrid.kt`, portrait and mixed paths) use the shared
  calculation. The list page inherits it through `MatrixMediaGrid`.
  No behaviour change for landscape artwork.
- [ ] Theme gap in the grid: `MatrixMediaGrid.kt` uses `MatrixLayout.gap`
  in its arrangement (`horizontalArrangement`) as well as in measurement,
  replacing the hardcoded 16 dp. Default-theme layout is unchanged.
- [ ] One safe-mode source: add a runtime-aware
  `MatrixSafeMode.effectiveColumns(prefs, safeMode: Boolean)` that
  returns the default when the runtime flag is on, else the saved value.
  All four call sites pass `repository.safeMode`: Home surface (replaces
  its inline `if`), the size settings page (replaces its inline `if`),
  the Search grid call, and the list-page call. Delete the prefs-only
  overload if no caller still needs it.
- [ ] Image sizing: verify the image request follows the new width —
  `MatrixMediaCard` already builds its `ImageRequest.size` from
  `artworkWidth`; confirm no decode path still assumes 144 dp (grep for
  the old constant and for fixed poster pixel sizes). Larger posters
  must decode at the larger rendered size; do not add a new decode path.
- [ ] Tests: focused JVM tests first. Update
  `MatrixLayoutTest.portraitListsFitAcrossTheViewportAtEveryUserSize`
  (its `poster <= 144.dp` assertion belongs to the old policy — replace
  it with the per-size ceiling assertion, do not delete the test) and
  check `largePortraitsKeepTheirExistingCapInsteadOfGrowingToFillTheRow`
  still passes unchanged. Add tests for: the four rendered widths at
  960 / 1920 / 640 dp windows; Home and Search producing the same
  preferred width from the same window + columns; the ceiling scale
  clamping at 0.75 / 1.25; effective columns returning the default in
  intent-only safe mode (runtime flag on, pref off) and the saved value
  otherwise. No behaviour change without a test naming it.
- [ ] Gates: full `./gradlew :core:test :app:testDebugUnitTest
  :app:lintDebug` green (lint 0 errors; pre-existing warnings only).
  Run `tools/audit_structure.py`. Run `impeccable detect` on the changed
  `.kt` files and note the known regex-only caveat. No PNG visual possible
  in this VM — true visual is NEEDS-TV.
- [ ] Phone (NEEDS-TV, leave unchecked): on the TV, each Card size step
  visibly changes poster size on Home and on Search, and the same size
  looks the same on both screens; Large posters are clearly larger than
  before; Small clearly smaller than Compact; no poster overflows or
  clips at the row edge; list pages match Search.
