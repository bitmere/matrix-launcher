# Matrix Launcher 2.5.0 — Stable

## Highlights

### 1. Robust D-pad Card Navigation & Row Focus Memory
- **Favorites Dock Focus Retention**: Added persistent `focusedFavId` (`rememberSaveable`) and `favRetained` (`FocusRequester`) passed as `fallback` to `MatrixFocusRow`. When moving from Card 1 to Card 2, navigating Down into media channels, and navigating back Up, focus cleanly returns to Card 2 instead of resetting to Card 1.
- **Direct Row-Entry Routing**: Utilizes `focusProperties { enter = { if (fallback != FocusRequester.Default) fallback else FocusRequester.Default } }` across all `MatrixFocusRow` instances. Vertical navigation directly resumes the row's remembered card without spatial snap artifacts or falling back to index 0.
- **LazyLayout Viewport Retention**: Configured `LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f)` in horizontal rows so nearby and retained items stay composed. Removed fragile `retainedVisible` check that previously dropped focus when scrolled cards moved outside the immediate viewport.
- **D-pad Boundary & Void Protection**: Added edge cancellation in the header (`FocusDirection.Up`) and bottom channels (`FocusDirection.Down`) preventing focus traps or escaping offscreen.
- **Predictable Back & Reset Actions**: Dedicated BackHandler resets row memory cleanly to the first card, and Home requests return directly to the top item.

### 2. Low-Resource TV Performance Optimizations
- **Halved Backdrop GPU Memory (-50%)**: Converted `MatrixBackdrop` texture generation from `RGBA_F16` (8 bytes/px, 7.37MB) to `ARGB_8888` (4 bytes/px, 3.68MB) with `Paint.DITHER_FLAG`, drastically reducing GPU memory pressure and bandwidth spikes during transitions.
- **Banner Artwork Cache**: `HomeArtwork` now renders proportional banners in `Bitmap.Config.RGB_565` (259KB each) and expands the in-memory LRU cache to 8MB (~32 banners vs 7), eliminating disk eviction churn during fast browsing.
- **Coil Pipeline & Overdraw Reduction**: Memory cache expanded to 20% heap in `MatrixComposeApplication`, disk cache to 64MB; disabled crossfades in `MatrixMediaCard` to prevent multi-card alpha overdraw on TV GPUs.
- **Hot-Path Allocations**: Added lazy instantiation in `MatrixArtwork.drawWithCache` so unfocused cards allocate 0 border/shader objects.

---

## Verification on TCL Android TV

- **Physical Device**: TCL Smart TV (`192.168.29.219:5555`) running Android TV.
- **Navigation Validation**: Verified via `dumpsys activity com.matrix.launcher --matrix-ui` and UI Automator XML dumps with zero screenshots.
  - Card 1 (`[116,694][428,870]`) → Card 2 (`[460,694][772,870]`) → Down to Row 1 (`homeRow=1, homeCard=2`) → Up to Row 0 (`homeRow=0, homeCard=1` restored).
- **Frame & GPU Benchmarks**:
  - 50th percentile frame time: **14ms** (60fps target achieved).
  - GPU render time: **4ms**.
  - Total GPU memory: **38.07MB** with 0 slow bitmap uploads.
- **Test Suite**:
  - `:core:test` and `:app:testReleaseUnitTest` passed cleanly.
