# Phase 0 tasks

- [x] Add only Haze core and blur 2.1.0 and attribution.
- [x] Check versioned API and tagged sources; document stack skew.
- [x] Float one bounded Home header above a source containing backdrop and rows.
- [x] Preserve controls and initial/Back requesters; reserve measured header space
      and account for the overlay in row reveal; keep key handling at common parent.
- [x] Add SDK/safe-mode/low-RAM decision coverage as plain JVM tests.
- [x] Run structure audit, diff whitespace check and Impeccable text detector.
- [ ] Coordinator: dependency resolution, Kotlin metadata compatibility, JVM tests,
      lint and optimized checks; ensure Compose has not silently upgraded.
- [ ] TV: XML/D-pad traversal and Back, favorites empty/populated, Now Playing
      appearing/disappearing, header focus return, held keys and rapid reversal.
- [ ] TV: settled screenshots, icon pixel bounds, contrast on bright/busy/black
      wallpapers and custom themes; API 26–30 and CHEAP static fallback.
- [ ] TV: newer TV/API 31 frame timings and memory vs baseline, wallpaper rotation,
      old TCL zero-live-effect verification based on actual device capabilities.
- [ ] Record adoption verdict only after all coordinator/device gates pass.
