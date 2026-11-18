# theme

## ADDED Requirements

- THE SYSTEM SHALL load a `theme.json` file from the app's files dir
  at startup into a ThemeState, mapping token names (`background`,
  `panel`, `surface`, `text`, `textSecondary`, `accent`, `radius`,
  `gap`, `focusScale`, `tileSize`) to values.

- THE DEFAULT tokens SHALL be the current values: colours from
  `home_design.xml`, radius/gap/focusScale from `MatrixLayout`,
  `tileSize` from `LIST_COLUMNS` (range 3–6), `accent` defaulting to
  the text colour and mapping to the colour scheme's `primary`.

- THE SYSTEM SHALL validate a theme file before applying it: colours
  must parse as #RRGGBB/#AARRGGBB; radius 0–32dp; gap 0–32dp;
  focusScale 1.0–1.08; tileSize 3–6; text/background contrast ≥4.5;
  textSecondary/background ≥3.0.

- WHEN a theme file fails validation, THE SYSTEM SHALL ignore the
  whole file, fall back to defaults, and record the reason for the
  Theme page to show.

- THE SYSTEM SHALL provide a Theme page in the settings registry,
  reachable from the Appearance page, showing the resolved tokens as
  plain-text status rows, with Apply (write + recreate), Reset to
  default (delete the file + recreate), and Import (Storage Access
  Framework open document, copy to filesDir/theme.json, validate,
  recreate) actions.

- THE THEME parser/validator SHALL be pure (String in, ThemeState
  out, no Context) and covered by JVM-only unit tests.

## Non-goals

- No WebView, no CSS, no theme gallery, no LAN install or QR install.
- No new permissions. Theme import uses the Storage Access Framework.
- No change to the focus outline alpha, the 1.015 focus scale
  default, or the backdrop behaviour — the theme only changes the
  token values.
