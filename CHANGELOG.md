# Changelog

## 2.7.10 — stable

- Order-screen fixes (PR #8): pressing OK on a channel row acts at once (the 400 ms first-press ignore is gone); hold-OK to show or hide a channel now uses a timed hold instead of depending on the remote sending key repeats; rows show ✓ shown / ○ hidden; gamepad A saves a move like OK.
- Settings rows no longer show a pale unfocused border (PR #8); the focused border is unchanged.
- Temporary diagnostic for the size-checkmark delay (PR #8): logging only, off by default.
- Tests: 291 JVM tests, all passing; lint 0 errors, 4 warnings.

## 2.7.9 — stable

- Impeccable design setup (PR #6): vendored skill (v4.10.0), Codex design hooks with a `.kt` detector, product context and design tokens in PRODUCT.md / DESIGN.md. Config/setup only; no app code changed.
- Tests: 786 JVM tests, all passing; lint 0 errors, 4 warnings.

## 2.7.8 — stable

- Impeccable P1 fixes (PR #4): AI search now shows a Siri-style orb (56dp luminous sphere, five-colour sweep swirl, one rotation per 6s, gradient confined inside the orb); theme changes apply live without restarting the app; a Cinemeta outage now says "Catalog unavailable — check connection" instead of "no results"; focused cards get a solid 2dp theme ring and a bolder title, and posters have loading and error states.
- Tests: 786 JVM tests, all passing; lint 0 errors, 4 warnings.

## 2.7.7 — stable

- Release pages now show only the APK: the SHA-256 moves into the release notes (`SHA-256: …`) and the updater reads it from there, with the `.sha256` sidecar as fallback for older releases (PR #2).
- Tests: 256 JVM tests, all passing; lint 0 errors, 4 warnings.

## 2.7.6 — stable

- The Nuvio-way updater reads GitHub Releases and checks APK size, SHA-256, package, version and signing certificate.
- The application ID changed to `com.matrix.launcher`. The first build signed with the new bitmere key requires a manual clean installation.
- GitHub Releases now serves the APK and checksum directly and supersedes the historical mirror.

## 2.7.5 — stable

- Cleanup and release gates (PR #14): docs corrected to Actions-only signing with the clean-install warning; CI gains a fail-closed `quality-gate` job; the release workflow is hardened (certificate pin, resumable publish, ephemeral signing material, anonymous verification); `tools/release_gate.py` validated the release and mirror before publishing.
- First release signed with the rotated key: this will NOT install over the TV's launcher — uninstall the old build and reinstall, then updates work normally.
- Tests: 260 JVM tests, all passing; lint 0 errors, 3 warnings.

## 2.7.4 — stable

- Update flow fixes (PR #13): release notes now open in a dedicated reader screen with Up/Down scrolling; installs now ask for confirmation (Update/Cancel) on API 31+ instead of installing silently; the launcher reopens itself after a successful install.
- Tests: 260 JVM tests, all passing; lint 0 errors, 2 warnings.

## 2.7.3 — stable

- Mirror filenames (PR #12): the release workflow wrote the APK with the v-prefixed name (Matrix-Launcher-v<version>.apk) in the filename and in latest.json, matching the earlier mirror files.
- Tests: 258 JVM tests, all passing; lint 0 errors, 3 warnings.

## 2.7.2 — stable

- Mirror automation (PR #11): the release workflow published the APK and latest.json to the public release mirror automatically, so in-app update checks could see new versions without a manual step.
- Repository docs organized: TODO and llms-full moved into docs/, CLEANUP and PROVENANCE moved into docs/, with new documentation and tool indexes.
- Tests: 258 JVM tests, all passing; lint 0 errors, 3 warnings.

## 2.7.1 — stable

- Audit fixes (PR #10): all 31 findings from the full-code audit of 2.7.0 addressed. Backups are now strictly validated against a key schema with exact types and value bounds, and the TMDB key is never exported or imported; AI and TMDB key fields are masked by default with an eye icon to reveal; the update installer is session-correlated with resolved settings fallbacks and safe retries; SIMKL add-by-ID no longer navigates away immediately; AI tool calls and response reads are bounded; non-Latin titles no longer collapse and media matching respects the media kind; the per-channel cap no longer discards better late candidates; the structure-audit tool is fixed.
- Tests: 258 JVM tests, all passing; lint 0 errors, 3 warnings (two deliberate `commit()` uses for failure detection, one Gradle-version notice).

## 2.7.0 — stable

- Manual update check: the About page now has a "Check for updates" row. It checked the public release mirror for a newer version, downloaded the APK with progress, verified its SHA-256 before installing, and installed it — silently on Android 12+ once the running build has the updater; the first update from an older build shows the system confirmation screen instead.
- Tests: 231 JVM tests, all passing; lint 0 errors, 0 warnings.

## 2.6.0 — stable

- Release pipeline: CI now runs the `:core` tests too, so a green run proves the whole suite; the release workflow's header no longer claims releases are debug-signed — they are signed with the permanent release key.
- Everything since 2.5.4 in one version: the test cleanup and slow-test fix (PR #5, #6), lint clean, and the small-wins bundle — capability-gated effects for old and new TVs, artwork hardening, and bounded channel, label and top-metas lookups (PR #7).
- Tests: 212 JVM tests, all passing; lint 0 errors, 0 warnings.

## 2.5.4 — stable

- Focus glow: the focused card's glass rim now revolves for 12 seconds when focus lands, then holds still — long enough to actually see it glow. Home still draws 0 frames at rest.
- Theme page: the ten token rows are now tappable — tap Focus zoom, Tile size, Corner radius, Gap, or any colour to cycle its value. The new value is saved to theme.json and applied straight away. All rows are reachable by D-pad now.
- Search posters: AI search no longer makes a second Cinemeta details fetch when only the IMDb rating is missing, so posters resolve faster.

## 2.5.3 — stable

- Performance refresh: removed the dead Cinemeta catalog fetch; AI search now dedupes repeated titles and memoizes lookups within one search; Home load no longer queues behind SIMKL work (separate locks); SIMKL feed sections load in parallel; web-search cache is bounded and DuckDuckGo + Wikipedia are fetched together; hot-path regexes are compiled once.
- Search refresh: a Refresh button in the search header forces fresh results, bypassing the 10-minute cache for that query (including AI search).
- AOT status rows are now plain text (title + fully wrapped subheader), after the info-card layout truncated the detail.
- Focus glow: the focused card's glass rim sweeps once when focus lands, then holds still — Home draws 0 frames at rest. The highlight (focus outline, 1.5% scale, backdrop change) is unchanged.
- Theme tokens: a theme.json file in the app's files dir + a Theme settings page (10 tokens, validated, contrast-checked).
- Settings backup export/import (Storage Access Framework); secrets are never exported or imported.
- Safe mode: after 3 crashes, or via the intent extra / settings toggle — SIMKL, AI and wallpaper off, default columns.
- Bench harness: tools/bench.py measures cold start, D-pad tour, idle frames, memory and key latency over adb.
- Tests: 212 JVM tests, all passing.

## 2.5.2 — stable

- Settings cleanup: all settings keys now live in one typed `MatrixPrefs` (same values, same `personal_home` file); AI API keys show only the last 4 characters; AOT status rows are info cards, not tappable tiles; settings apply instantly through a `StateFlow` snapshot instead of the preference-revision workaround.
- Code split for maintainability: the 1,366-line search screen and 948-line AI client are split by feature; settings pages live in a registry; 156 settings strings moved to resources (English).
- Permanent release signing: releases are now signed with one kept release key instead of a per-machine debug key, so future updates install cleanly without uninstalling first.

## 2.5.0 — stable

- D-pad Card Navigation & Row Focus Memory: Fixed card focus resetting to card 0 when navigating between rows. Moving Card 1 -> Card 2, down to lower rows, and back up preserves Card 2 focus cleanly.
- Direct Row-Entry Routing: In `MatrixFocusRow`, `focusProperties.enter` cleanly routes to the row's remembered `fallback` focus requester instead of falling back to default or Euclidean bounds.
- D-pad Boundary Safety: Added edge direction cancellation for header (`FocusDirection.Up`) and bottom channels (`FocusDirection.Down`) preventing focus traps or escaping offscreen.
- LazyLayout Pre-caching: Configured `LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f)` in horizontal rows so retained and nearby cards stay composed; removed fragile `retainedVisible` layoutInfo check.
- Halved Backdrop GPU Memory (-50%): Converted `MatrixBackdrop` texture generation from `RGBA_F16` to `ARGB_8888` with dithering, reducing GPU memory and texture bandwidth spikes.
- Banner Artwork & Coil Optimization: `HomeArtwork` now renders proportional banners in `RGB_565` (259KB each) with expanded 8MB in-memory cache; disabled crossfades on media cards for smooth 60fps scrolling on TV hardware.
- Fully verified on physical TCL Android TV with UI Automator/dumpsys dumps and zero screenshots.
- [Release notes](docs/archive/RELEASE-2.5.md).

## 2.3.1 — stable

- Siri 3D Glowing Orb: hardware-accelerated animated glass sphere with counter-rotating fluid ribbons (magenta, electric cyan, mint) and additive incandescent white core (`BlendMode.Screen`) matching official Apple Siri aesthetics.
- Real-time AI Reasoning & Activity Stream: transparent chronological stream of live operations (`✓ Analyzing prompt`, `✓ Executed MCP web_search`, `✓ Live facts grounded`, `▶ Resolving official 4K posters & Cinemeta metadata`) replacing static/generic loading placeholders and removing placeholder cards.
- Dynamic AI Discovery Suggestions: real-time trending titles and topics queried from Cinemeta top catalog and web search engine without hardcoded lists.
- In-process Model Context Protocol (MCP) tool engine (`MatrixMcpEngine`): live `web_search`, `tv_context`, and `lookup_stream_availability` tools dynamically invoked by AI models to formulate queries, query installed TV streaming apps, and verify Cinemeta stream availability.
- Multi-turn MCP agentic loop: supports iterative search turns across OpenAI-compatible providers, Neuralwatt, and Google Gemini with native function calling.
- Dynamic temporal anchoring: models receive real-time calendar context (e.g. November 2026) to enforce strict 2025/2026 recency constraints for "latest/recent" queries.
- Clean card metadata: removed redundant `"AI Pick · "` prefix on discovery cards to eliminate text ellipsis on TV displays (`2026 · SIMKL 8.9`).
- Search experience & animations: dynamic animated progress container with stage badges (MCP Grounding, AI Reasoning, Artwork Resolution), animated text transitions, and shimmering 5-column poster card skeletons while searching.
- Animated results reveal: smooth fade/slide-in transition with total results header (`12 titles found · Google Gemini`).
- Recent searches & quick suggestions: persistent search history with "Clear all" button, one-click D-pad chips, and AI suggested query prompts.
- Robust Gemini function calling: supports arbitrary part indices for `functionCall` and full tool schema parity.
- [Release notes](docs/archive/RELEASE-2.3.md).

## 2.3.0 — stable

- Built-in In-App Web Search & MCP Tool Engine (`MatrixWebSearchEngine`): free live DuckDuckGo HTML & Wikipedia search directly from the TV without paid search API subscriptions.
- Direct Google Gemini Provider integration: native `generativelanguage.googleapis.com` support for `gemini-2.5-flash`, `gemini-2.5-pro`, and `gemini-2.0-flash` with structured JSON mode and connection testing.
- Neuralwatt live web grounding: seamlessly feeds real-time 2025/2026 web facts into Neuralwatt `deepseek-v4.1-flash` for 100% free live web recommendations.
- [Release notes](docs/archive/RELEASE-2.3.md).

## 2.2.0 — stable

- Fix D-pad long-press execution on remote key release: capture-phase key interception isolates options dialogs.
- AI Search upgrades: dual-source IMDb ratings on discovery cards, 2-line recommendation reasons, expanded 10–14 title results with 4096 tokens.
- Multi-provider AI search support: OpenRouter live web search plugin, Perplexity Agent API (`/v1/responses`), and per-provider API key persistence.
- Voice recognition enhancements with glowing mic indicator.
- [Release notes](docs/archive/RELEASE-2.2.md).

## 2.1.0 — stable

- Manual reload progress/completion feedback, with retained Settings result and no stale queued notice.
- Shared Home row/header horizontal boundaries and correct Edit Favorites focus bookkeeping.
- [Release notes](docs/archive/RELEASE-2.1.md).

## 2.0.0 — stable

- Artwork-coloured outward Home glow, clear translucent controls and dock.
- Readable settings explanations and fewer unnecessary control allocations.
- Slim translucent Continue Watching progress with watched-percentage accessibility state.
- Shared dynamic row/grid geometry retained; current docs cleaned and historical evidence archived.
- [Release scope and limits](docs/archive/RELEASE-2.0.md), [validation](docs/VALIDATION.md).

## 1.3.0 — stable maintenance

- Fix hidden-result/empty-gallery focus, reuse derived lists and stop cancelled image copies.
- Clarify wallpaper breadcrumbs and remove obsolete native-picker query.
- [Review scope, tests and limitations](docs/archive/RELEASE-1.3.md).

## 1.2.0 — stable

- Finalize the tested beta 1–7 changes: remote-friendly wallpaper browsing/import/share and saved selection, configurable media targets, consistent controls and upgrade-safe Matrix namespace.
- Refresh current documentation and retain earlier validation as history. No new feature or Home-mode changes beyond beta 7. [Release notes](docs/archive/RELEASE-1.2.md).

[Earlier prerelease history](docs/archive/CHANGELOG-THROUGH-1.1.1-BETA7.md).
