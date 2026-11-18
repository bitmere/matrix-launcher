# Tasks

1. Focus glow: in `MatrixButton.kt`, change `rememberMatrixFocusRotation` to sweep
   0→1080 over 12 s (three 4 s revolutions) when focus lands, then hold; update
   the doc comment (1080 equals 0 for the periodic glow shader).
2. Theme presets (pure, in `MatrixTheme`): preset lists for focus scale
   (1.00, 1.015, 1.03, 1.05, 1.08), tile size (3–6), radius (0, 8, 16, 24, 32),
   gap (8, 12, 16, 20, 24), and one preset list per colour token; a generic
   `cycle(values, current)` helper that returns the next value, wrapping.
3. Theme save (pure + IO): `MatrixTheme.encode(state)` builds the theme.json
   string with the same ten keys `parse` reads; `MatrixTheme.save(filesDir,
   state)` writes it. Keep colours as #AARRGGBB and numbers as numbers.
4. Theme page: replace the ten `isStatus` token rows with tappable rows. A tap
   computes the next value, saves theme.json on IO, applies the state, writes
   LIST_COLUMNS for the tile size, and restarts Home (the Apply button's
   existing mechanism). Update the page description string to explain tapping
   a row. Apply / Import / Reset unchanged.
5. Search posters: in `MatrixSearchRepository.resolveCinemeta`, narrow the
   second meta fetch so it runs only when the poster or the year is missing,
   not when only the IMDb rating is missing; update the step comment.
6. Tests: in `MatrixThemeTest`, cover `encode`/`save` round-trip (a saved file
   loads back to the same values with no ignored reason) and that every preset
   in every list round-trips through `parse(encode(...))` with no ignored
   reason. Cover `cycle` wrapping.
7. Build: `:core:test testDebugUnitTest lint assembleDebug` in the fixes
   checkout; expect 202 tests (or more, with the new theme tests) green and
   lint 0 errors.
