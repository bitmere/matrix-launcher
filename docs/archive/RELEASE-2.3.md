# Matrix Launcher 2.3.1 / 2.3.0

## Highlights

### 1. In-Process Model Context Protocol (MCP) Tool Engine (`MatrixMcpEngine`)
- **Agentic Tool Calling**: AI models dynamically formulate their own search queries, inspect TV platform context, and check media catalog availability rather than relying solely on static system prompt injection.
- **Tools Schema**: Implements standard OpenAI function-calling schema and Google Gemini `functionDeclarations` schema for:
  - `web_search`: Live search via DuckDuckGo HTML and Wikipedia API for fresh theatrical and streaming releases.
  - `tv_context`: Queries `PackageManager` for installed streaming apps (Nuvio, Stremio, Netflix, Hotstar, Prime, JioCinema, YouTube) and calendar date.
  - `lookup_stream_availability`: Validates stream and IMDb availability in Cinemeta / Stremio before finalizing recommendations.
- **Multi-Turn Agentic Loop**: Supports iterative search turns (up to 3 turns) with DSML and structured `tool_calls` handling.
- **Dynamic Temporal Anchoring**: Calendar dates dynamically anchored (e.g. November 2026 / 2026) ensuring queries for "latest" strictly return 2025/2026 releases.

### 2. In-App Web Search Engine (`MatrixWebSearchEngine`)
- **Zero-Cost Live Internet Access**: The launcher performs live web searches directly on the TV via DuckDuckGo HTML and Wikipedia Search API without requiring paid third-party search APIs, subscriptions, or proxies.
- **Smart Grounding Context**: Automatically extracts 4–6 clean, relevant snippets, release years, OTT availability, and cast information.
- **In-Memory Query Cache**: 1-hour cache prevents repetitive network lookups for common search queries.
- **Fail-Safe Fallback**: Connect and read timeouts (3.5s) ensure the launcher never crashes or hangs if home Wi-Fi is temporarily interrupted; the AI gracefully proceeds using its base knowledge.

### 3. Direct Google Gemini Provider Integration
- **Direct Google API**: Native endpoint integration (`generativelanguage.googleapis.com`) using standard Google AI Studio API keys (`AIzaSy...`).
- **Supported Models**: `gemini-2.5-flash` (recommended default), `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`.
- **Structured JSON Mode**: Uses `responseMimeType: "application/json"` in `generationConfig` to guarantee valid JSON recommendation payloads.
- **Dedicated Key Storage & In-App Testing**: Independent preference isolation (`ai_api_key_GEMINI`) and instant connection test via `/models?key=...`.

### 4. Neuralwatt + In-App Web Search Synergy
- Neuralwatt's free `deepseek-v4.1-flash` model receives fresh live web facts injected directly into its context window.
- Delivers up-to-date 2025/2026 theatrical and OTT recommendations at **₹0 / $0 total cost**.

## Verification
- Unit test suite passed (126 tests completed, 0 failed).
- R8 minification, resource shrinking, and full release checks passed (`scripts/build.ps1 -Checks`).
- Optimized APK: **1,842,878 bytes (1.76 MiB)**, `versionCode 124`.
- SHA-256: `C9B0B01A0AB7AEE07FEDDFFC606F02CE8101111CDD3D9AB8DE3C086E78552C11`.
