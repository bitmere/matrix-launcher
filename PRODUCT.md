# Matrix — product context

## Audience

People using an Android TV launcher on their television, navigating with a
remote and D-pad. Old and new TVs alike, including low-RAM devices.

## Purpose

A fast, low-memory, native Compose TV launcher: an app grid, channels,
search (keyless Cinemeta, SIMKL, optional TMDB with the user's own key),
natural-language AI discovery, and wallpapers. Settings behind the avatar,
no phone UI assumptions.

## Constraints

- D-pad first. Every action must be reachable and visible with a remote.
  Focus state is always obvious.
- Small footprint. Must run on old TVs (minSdk 26) and low-RAM devices,
  which fall back to cheaper Home effects with wallpaper and animated
  focus effects disabled.
- Package `com.matrix.launcher`, MIT license. Distributed through GitHub
  Releases only, no mirrors. The in-app updater reads the Releases API.
- API keys (AI, TMDB) are the user's own, entered on-device, masked by
  default with an eye icon to reveal.
- No functionality is removed to save space. Safe mode and automatic
  fallback after failed starts must keep working.
