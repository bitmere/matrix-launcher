# Matrix Launcher — Roadmap & TODO

## Completed in v2.3.0: Direct Google Gemini & In-App Web Search (MCP)

- [x] **Direct Google Gemini AI Provider Integration**:
  - Added `MatrixAiProvider.GEMINI` targeting `generativelanguage.googleapis.com/v1beta`.
  - Native models: `gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`.
  - Structured JSON output mode (`responseMimeType: "application/json"`).
  - Model catalog discovery (`/models?key=...`) and connection testing.
  - Dedicated API key storage per provider/model.
- [x] **In-App Web Search Engine (`MatrixWebSearchEngine`)**:
  - Real-time search directly from the TV via DuckDuckGo HTML and Wikipedia Search API.
  - Automatic prompt injection with 1-hour query cache.
  - Zero-cost live internet grounding for Neuralwatt, Gemini, DeepSeek, and all providers.
  - Settings toggle: "Live Web Search (MCP)".

---

## Implemented local MCP tools (source review: 2026-11-16)

- [x] `tv_context()`: `MatrixMcpEngine.getTvContext` returns date/year and installed streaming apps; `MatrixAiTools.kt` records tool progress and dispatches execution.
- [x] `lookup_stream_availability(title, year)`: dispatches a Cinemeta catalog lookup through `CinemetaClient.checkAvailability`. The implemented arguments are title/year, rather than the earlier proposed IMDb ID.
- [x] Two-way tool results: `MatrixAiProviders.kt` returns tool results to Gemini for a final response and runs bounded iterative turns for OpenAI-compatible providers. `MatrixAiTools.kt` shares execution/progress handling.

`AiBoundsTest.toolsHaveOneActionOwnerAndBoundedFanOut` covers action ownership, bounded concurrent execution, result ordering and oversized-call rejection using fake execution. This source review does not establish a passing test run or live provider/TV integration.

## Pending verification and extensions

- [ ] Verify provider tool calling and installed-app context on supported old and current TVs with live services; record provider/model and device evidence without credentials.
- [ ] Verify actual playable stream links from configured Stremio addons before promising one-click playback. Cinemeta catalog presence alone does not establish addon stream availability.
- [ ] Generalize Gemini beyond its current tool exchange plus final-response flow if repeated tool turns are needed; retain bounded execution and existing provider behavior.
