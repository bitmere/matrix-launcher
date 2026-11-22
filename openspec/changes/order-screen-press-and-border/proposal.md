# Order-screen press handling + unfocused control border — Matrix 2.7.9

Fix two user-reported bugs from Matrix 2.7.9 on the TV (2026-11-22) and add a
temporary diagnostic for a third. Investigation (code, main @ `605fd6ed`,
Astra cross-checked) is the evidence; nothing here is a blind fix except the
diagnostic, which is deliberately not a fix at all.

## 1. Order screen drops presses and hide/show never fires (bug 1)

The channels order page (`MatrixOrderScreen.kt`) has three press-handling
defects, all in one place:

- **The first OK press within 400 ms of opening the page is silently
  dropped** (`MatrixOrderScreen.kt:40` records `openedAt`; `:89` runs
  `begin()` only `if (uptimeMillis() - openedAt >= 400L)`). A fast user
  pressing OK as the screen appears gets nothing, with no feedback, and
  concludes selection is broken.
- **OK on a row does not "select" anything.** Short OK means *pick up to
  move* (`begin()`), not check/select. The only affordance is a hint line
  (`:81-82`) and "Shown on Home"/"Hidden on Home" detail text (`:105`).
  There is no checkbox/radio on order rows.
- **Hide/show by hold-OK probably never fires from the user's remote.**
  Visibility toggling exists only as `onLongClick` on the tv-material3
  `MatrixButton` (`:91-94`). The built-in long-click fires on a key DOWN
  with `repeatCount == 1` — it requires the remote to deliver a key-repeat
  event while OK is held. Remotes that send only an initial DOWN and an
  eventual UP can never trigger it. The codebase already works around this
  exact unreliability on home cards: `tvCardClickable`
  (`MatrixCards.kt:87-114`) times holds with a manual 380 ms timer on the
  initial DOWN instead of trusting the built-in long-press. The order
  screen does not use that workaround.

The size/limits menu path is *not* broken for a clean press (Dialog and
Menu preview handlers pass a fresh DOWN through; `select(index)` writes
prefs). What the user feels there is the 400 ms drop plus bug 2 making a
successful press look like nothing happened.

A connected find: while a row is being moved, the order screen's key
handler covers CENTER/ENTER/NUMPAD_ENTER but **not BUTTON_A**
(`MatrixOrderScreen.kt:63-71`) — a gamepad user's OK does nothing mid-move.

**Fix:** replace the order screen's press handling with one explicit
lifecycle reusing the proven `tvCardClickable` pattern: accept fresh short
presses immediately (drop the `openedAt` rejection), time holds with the
~380 ms timer instead of depending on key repeats, suppress the
short-click after a hold, keep move/save behaviour. The shared helper
also guards against an unmatched UP: a press whose DOWN went to another
row (focus moved mid-hold) is swallowed, never acted on — review finding,
same hardening benefits the home cards that share the helper. Add a
visible selection affordance on order rows (✓/○ like
`MatrixMenu.kt:132`), shown only where visibility applies
(`page.visibility`). Cover BUTTON_A in the move key handler. Keep the
hint text accurate. Size: small–medium. Risk: medium — touches
input/focus handling on the move flow; needs a TV pass by the user.

## 2. White border on every settings row (bug 3)

Every `MatrixButton` — every row on every settings menu/order page — gets
an **unfocused border** from `matrixControlBorder`
(`MatrixButton.kt:244-251`), built by `MatrixControlStyle.stroke`
(`MatrixButton.kt:69-71`): 0.75 dp, a linear gradient of `onSurface` at
60% / 12% / 36% alpha. On the dark theme `onSurface` is near-white, so
every row shows a pale white border at all times, focused or not. The
focused border is a *separate* 1.5 dp solid `onSurface`
(`MatrixButton.kt:249-250`). The settings screens have no glow at all, so
the border the user sees there is this stroke.

Ruled out: home cards (unfocused border is explicitly `Border.None`,
`MatrixCards.kt:153-155`); the glow (`MatrixGlassBorder`,
`MatrixCards.kt:434-445`, focused card only); PR #4 (touched
`MatrixCards.kt` only, never `MatrixButton.kt`). The poster letterbox
background (`MatrixCards.kt:449`, `surfaceVariant` behind a `Fit` image)
is a separate Home-only candidate — untouched unless the user confirms it
from a screenshot. Dimmer unfocused titles are by design
(`cardTitleColor`, `MatrixCards.kt:128-129`), not a bug.

**Fix:** set the ordinary unfocused control border to `Border.None` in
`matrixControlBorder`, keeping the 1.5 dp focused border. The destructive
unfocused border (1 dp `onErrorContainer` at 55%) stays — it is a
meaningful affordance, not white, and not what the user reported. The
text-field surface (`MatrixSettingsComponents.kt:67`) also uses
`MatrixControlStyle.stroke`, but a field needs a visible edge to read as
a field; it stays. Size: tiny. Risk: low — one visual check that
unfocused rows still read as separate rows (spacing + fill already
separate them).

## 3. Selection doesn't show until back-and-return (bug 2) — diagnostic only

No confirmed break in the static code; the whole update chain reads
correct (same prefs instance `ComposeHomeRepository.kt:18` →
`settingsState` `:20`; listener retained and bumps revision
`MatrixSettingsState.kt:23-28`; `MatrixHomeApp` collects it
`MatrixHomeScreen.kt:63-64`; pages re-read prefs at build
`MatrixSettingsPages.kt:206-208`, `:187`; menu renders
`entry.checked ?: entry.selected` with no remembered checkmark state,
`MatrixMenu.kt:129-133`). A blind fix would change the wrong thing.

**Diagnostic, not a fix:** one clearly-marked `MatrixSettingsDiag`
object (single file, one `enabled` flag, default off, lazy detail
strings so nothing is built or read while disabled) with one-line
`// DIAG-BUG2` call sites tracing, after ONE OK press on the size page:
persisted value written → listener revision bump (with the changed key)
→ parent revision seen by `MatrixHomeApp` → `def.build` result →
built menu's selected flags → what the menu actually composed with.
The user runs a diagnostic build on the TV and sends the log back; the
flag flips on for that build only, and the whole diagnostic deletes in
one step. No behaviour changes.
