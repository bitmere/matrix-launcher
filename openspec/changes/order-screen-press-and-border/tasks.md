# Tasks — Order-screen press handling + unfocused control border

- [ ] Order press lifecycle (bug 1): in `MatrixOrderScreen.kt`, replace the
  `MatrixButton` `onClick`/`onLongClick` pair with the shared press
  lifecycle: hoist `tvCardClickable` (`MatrixCards.kt:87-114`) from
  `private` to `internal` and apply it to each order row, passing
  `onClick = { begin(entry.id) }` and the visibility toggle as
  `onLongClick` (null when `!page.visibility` or moving). Pass
  `onLongClick = null` to `MatrixButton` so the built-in repeat-dependent
  long-press cannot double-fire. Delete the 400 ms `openedAt` rejection
  and the now-unused `openedAt` value. Keep move/save behaviour, the
  hint text, and the `hidden` state update before the prefs write.
  Harden the shared helper: an UP only counts if that row saw its DOWN
  (swallow orphan UPs; reset on focus loss) — review finding, same
  hardening benefits the home cards that share the helper.
- [ ] Order row affordance (bug 1): on rows where `page.visibility` is
  true, show ✓ when the row is shown on Home and ○ when hidden, trailing
  the row text like `MatrixMenu.kt:132-133` (titleSmall,
  `clearAndSetSemantics`). The existing "Shown on Home"/"Hidden on Home"
  detail text stays.
- [ ] BUTTON_A mid-move (bug 1): add `Key.ButtonA` to the move key
  handler's save branch (`MatrixOrderScreen.kt:63-71`) alongside
  CENTER/ENTER/NUMPAD_ENTER.
- [ ] Unfocused border (bug 3): in `matrixControlBorder`
  (`MatrixButton.kt:244-251`), return `Border.None` for the ordinary
  (non-destructive) unfocused border. Keep the 1.5 dp focused border, the
  destructive unfocused border, the glow, and the home-card borders
  untouched. Do not touch `MatrixControlStyle.stroke` (still used by the
  text-field surface) or the poster letterbox background.
- [ ] Bug-2 diagnostic: add `MatrixSettingsDiagnostics.kt` with one
  `MatrixSettingsDiag` object (single `enabled` flag, default off,
  `Log.d` under tag `MatrixSettingsDiag`, detail strings lazy so nothing
  is built or read while disabled). Add one-line `// DIAG-BUG2` call
  sites: (a) size `onSelect` — value written + revision before/after;
  (b) `MatrixSettingsState` listener — changed key + new revision on
  each prefs change; (c) size def build via `SideEffect` (a direct
  `StateFlow.value` read in composition is a lint error) — effective
  columns read + selected flags computed; (d) `MatrixSettingsPage`
  after `def.build` via `logMenuBuilt` — Menu entries' selected flags;
  (e) `MatrixHomeApp` `LaunchedEffect` on the collected revision —
  the parent revision; (f) `MatrixMenu`'s `SideEffect` — the selected
  flags the menu actually composed with. No behaviour change. Document
  the diagnostic-build run steps in the parent report.
- [ ] Tests: focused JVM tests first for each change where testable
  without a device (press-lifecycle decision logic extracted to a pure
  helper if the composable-local code cannot be tested directly; border
  selection for ordinary/destructive/focused roles; diagnostic flag
  defaults off and logging is a no-op when off). No behaviour change
  without a test naming it.
- [ ] Gates: full `./gradlew :core:test :app:testDebugUnitTest
  :app:lintDebug` green (lint 0 errors; pre-existing warnings only).
  Run `tools/audit_structure.py`. Run `impeccable detect` on the changed
  `.kt` files and note the known regex-only caveat. No PNG visual possible
  in this VM — true visual is NEEDS-TV.
- [ ] Phone (NEEDS-TV, leave unchecked): on the TV, press OK on a
  channel row immediately after opening the order page (selection
  registers); hold OK ~half a second (row hides/shows with ✓/○
  updating at once); size pick shows its checkmark without leaving the
  page; settings rows show no white border unfocused and keep the
  focused border; move flow still saves/cancels as before.
