# Shared card-size policy — Matrix 2.7.10

The Card size setting (Settings → Appearance → Card size, Large / Medium /
Compact / Small → `list_columns` 3 / 4 / 5 / 6) reaches both screens, but it
does not do what the user expects. Debug by Codex (Astra), session
`01a20328`, transcript `/tmp/matrix-card-size-codex.log`, on main @ `4e032d78`:

1. **One flat ceiling flattens all four sizes.** Portrait posters go
   through `MatrixLayout.mediaWidth` (`MatrixLayout.kt:41`), which caps
   every size at 144 dp. On a wide viewport (e.g. a 4K TV, 1920 dp window)
   the slot-derived width exceeds 144 dp for every size, so Large, Medium,
   Compact and Small all render identical 144 dp posters — changing the
   setting visibly changes nothing. Below the cap the sizes do differ
   (960 dp window: Large 144, Medium ~130, Compact ~102, Small ~83 dp).
2. **Home and Search compute the same poster two different ways.**
   Search derives its slot from a 10 dp grid inset inside a 30 dp panel
   inset (80 dp total); Home derives its slot from the 52 dp channel
   inset (104 dp total). Below the cap, search posters come out larger
   by `0.625 × (104 − 80) / columns` — only 2.5–5 dp, and the cap can
   erase even that — but the two screens can disagree, and they fit
   differently (search `fittingColumns` centers the card in its cell;
   home `rowFit` preserves the width and spreads spare space into gaps).
3. **The grid hardcodes its gap.** `MatrixMediaGrid.kt:39` lays out at a
   hardcoded 16 dp while the measurement math uses the theme gap
   (`MatrixLayout.gap`, default 16 dp). Identical on the default theme;
   wrong on any other theme.
4. **Safe mode has two sources.** Home reads the runtime flag
   (`repository.safeMode`, set at startup from the persisted pref, the
   launch intent, and the crash counter — `ComposeHomeActivity.kt:164-165`),
   while Search and the list page read only the persisted pref
   (`MatrixSafeMode.effectiveColumns(prefs)`). In intent-only safe mode
   the two screens can disagree on columns.

## What this change does

- One shared preferred portrait-width calculation, computed from a common
  measured window width and a common sizing inset, used by Home rows,
  the Search grid, and the list page. Each container then fits that width
  into its own layout. This removes the 2.5–5 dp cross-screen difference.
- The single 144 dp ceiling becomes an explicit four-size portrait
  policy: each size has its own ceiling, scaled by window width within
  safety bounds. Each size step is visibly distinct at every viewport,
  Large is genuinely larger than today, and Small is genuinely smaller
  than the other sizes. The same pref key and values are kept
  (`list_columns` 3–6); nothing else about the setting changes.
- The grid uses the theme gap in both measurement and arrangement.
- Effective columns come from one runtime-aware safe-mode source.

## Target widths (frame widths, dp)

Base ceilings at the 960 dp reference window, scale =
`(windowWidth / 960).coerceIn(0.75, 1.25)`:

| Size (columns) | Base ceiling | Rendered at 960 dp | Rendered at 1920 dp |
|---|---|---:|---:|---:|
| Large (3) | 168 | 168 | 210 |
| Medium (4) | 144 | 126 | 180 |
| Compact (5) | 120 | 99 | 150 |
| Small (6) | 96 | 81 | 120 |

Rendered width = `min(slotWidth × 0.625, ceiling)`, where the slot comes
from the shared calculation (`cardWidth(windowWidth, columns,
channelInset)`). At 960 dp only Large changes versus today (144 → 168);
Medium / Compact / Small keep their slot-derived widths (126 / 99 / 81)
and become identical on both screens. At 1920 dp the four sizes spread
to 210 / 180 / 150 / 120 instead of all collapsing to 144. Below the
ceiling the width is slot-derived, so smaller viewports keep working
unchanged (640 dp window: 105 / 76 / 59 / 47.5). The scale clamp bounds
growth on very wide screens (never above 1.25×) and shrinkage on very
narrow ones (never below 0.75×), so posters can never overflow the
viewport; the existing container fit (`coerceAtMost(slotWidth)`) stays
as a backstop.

Why these numbers: 168 / 144 / 120 / 96 are 24 dp apart — a visible
step at any density — and each ceiling sits just above the next size's
slot-derived width at the reference window, so no size is wasted. Large
168 is 17% larger than today's 144; Small 96 keeps the smallest posters
clearly smaller than Compact's 120. Poster height follows at 1.5× width,
so Large rows get taller (252 dp poster at 960 dp, 315 dp at 1920 dp);
that is the intended trade for a Large setting. Image requests are
already sized from the rendered artwork width
(`MatrixMediaCard` builds its `ImageRequest.size` from `artworkWidth`),
so larger posters decode at the larger size with no new decode path —
this must be verified, not assumed, in implementation.

## Out of scope

- The focused-card white border and the glow (separate user report,
  separate pending approval) — untouched.
- The focused "Om" looking much larger than "Omaha" in the user's photo:
  Codex could not explain it from code (focus zoom is 1.5%, the
  cross-screen difference is 2.5–5 dp). It still needs a TV measurement
  (same poster, focused vs unfocused, across size settings) and is NOT
  fixed by this change.
- No pref key/value changes, no size-menu label changes, no dock or
  app-grid sizing changes, no new decode paths, no lost functionality.
