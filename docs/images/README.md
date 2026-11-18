# Matrix public-launch images

These are **code-based mockups, not device screenshots or Compose runtime renders**. Artwork, app entries, media titles, scores, dates and playback progress are illustrative. Placeholder blocks deliberately avoid fabricated movie posters or live account data. The Home image is a taller illustrative content extent; a TV shows a viewport and scrolls to later rows. Icons/focus effects are a static approximation, not remote-focus verification.

| File | Purpose | Production |
| --- | --- | --- |
| `matrix-hero.svg` / `.png` (1280 × 360) | Centered README brand image | Existing `matrix_banner.xml` paths plus Outfit brand copy, on the default Matrix background. |
| `matrix-home-mockup.svg` / `.png` (1280 × 1100) | README Home layout illustration | Header tools, clock, favorite dock, landscape continue-watching cards and portrait SIMKL cards constructed from the code roles below. |

## Source map

- `app/src/main/java/com/matrix/launcher/MatrixTheme.kt`: defaults read by the generator, including background `#000000`, panel `#1C1E24`, surface `#242B34`, text `#FFFFFF`, secondary `#D1D5DF`, radius 24 and gap 16.
- `MatrixTvTheme.kt` and `res/font/outfit.ttf`: bundled Outfit light/regular/medium weights and relevant 56/42/22/17/15/14/12/11 type roles. Text becomes vector outlines so PNG/SVG exports use the same font without requiring a font install. The fallback star is outlined separately because Outfit lacks U+2605.
- `MatrixHomeSurface.kt`: welcome/time/date, six toolbar slots, 48/55dp text insets, 100dp header spacer, 46dp dock outer inset, and favorite/channel hierarchy. Five example favorites use the five-column case; there is no claim these apps/banners were read from a TV.
- `MatrixLayout.kt`, `core/.../HomeCardSizing.java`: shared spacing, dock inset, card corner ratio 24:14, landscape sizing and 144dp 2:3 poster width. The illustration expands the content vertically to show both rows; it is not pixel-identical to a TV density/viewport.
- `MatrixButton.kt`: circular 54dp toolbar geometry and the dock’s white-opacity glass stops `.15/.035/.08`. Used here because they are the app’s actual styles.
- `MatrixCards.kt`: fallback app tiles, portrait/landscape cards, type pill, rating background `#EE111524`, gold border `#F59E0B`, score text `#FFD166`, and progress track. Example numbers illustrate the renderer, not verified ratings.
- `res/drawable/matrix_banner.xml`: unchanged original green mark/background paths, reused directly. `res/mipmap-anydpi/matrix_launcher.xml` references the related `matrix_mark.xml` adaptive foreground; no replacement logo is invented.
- Existing `ic_search`, `ic_input`, `ic_wifi`, `ic_home_refresh`, `ic_apps` and `ic_settings` Android vector paths are reused for actual toolbar/fallback icon shapes. No third-party app logo is fabricated.
- `MatrixSearchScreen.kt`, `MatrixSearchOrb.kt`, `MatrixMediaRouter.kt`, `HomeSections.java` and `MatrixSearchRepository.kt`: verify the search, media handoff and optional SIMKL/AI copy. No generated search-results screen or fake AI answer is presented.

## Reproduce

Run `python3 docs/images/generate.py` from the checkout. It requires Python 3, fontTools, and system librsvg/cairo shared libraries. The script reads only this checkout, uses no network, writes these six image files, outlines the bundled font and rasterizes each SVG through librsvg/cairo. No Android build, screenshot, image-generation service, account or device is involved. Regeneration with the same renderer/fontTools versions is deterministic.

The PNGs are convenient raster exports; the SVGs are self-contained vector sources with accessible titles and labeled outline groups. The existing MIT/upstream, Outfit OFL and icon attributions still apply: [MIT](../../LICENSE), [Outfit](../../FONT-LICENSE-OFL.txt), [icons](../../ICONS-LICENSE-FLUENT-EMOJI.txt), [provenance](../PROVENANCE.md).
