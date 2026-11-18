# Matrix Launcher 2.2.0

## Highlights

### 1. D-Pad Long-Press Safety
- Fixes D-pad long-press execution on remote button release: on Android TV (`androidx.tv.material3.ClassicCard`), trailing `ACTION_UP` events previously clicked the first item of a newly opened dialog (e.g. "Remove favorite" or "Open").
- Implemented capture-phase key interception in `tvCardClickable` with a 380ms threshold, consuming `ACTION_DOWN` (and repeats) and consuming the trailing `ACTION_UP`.
- Added menu and order screen open-time debouncing (400ms guard) to ensure clean remote interactions on all Android TV remotes.

### 2. Enhanced AI Search Discovery & IMDb Ratings
- **Dual-Source IMDb Ratings**: Search repository extracts verified IMDb ratings directly from Cinemeta (`meta.imdbRating`) and SIMKL (`ratings.imdb.rating`), formatted cleanly as `AI Pick · <year> · IMDb <rating>`.
- **2-Line Recommendation Reasons**: Expanded the card summary layout from 1 line to 2 full lines (`minLines = 2, maxLines = 2`), preventing premature truncation of AI rationale.
- **Expanded Discovery Results**: Raised prompt expectations to request 10–14 titles with an increased token budget (`maxTokens = 4096`), populating dense, browsable multi-row grids.
- **Direct Streaming Integration**: Selecting any movie/show result seamlessly opens Nuvio or installed media apps with stream links and artwork.

### 3. Multi-Provider AI Search & Persistence
- **OpenRouter Live Web Search**: Integrated the OpenRouter `:online` engine and web search plugin (`plugins: [{id: "web"}]`) for real-time web claims, latest release dates, and streaming sources.
- **Perplexity Agent API**: Updated Perplexity integration to the `/v1/responses` Agent API endpoint (`input`, `instructions`, and `output`).
- **Independent Key Retention**: Dedicated per-provider and per-model preferences (`ai_api_key_<provider>` and `ai_api_key_<provider>_<model>`) preserve user API keys when switching between Neuralwatt, OpenRouter, and Perplexity.
- **In-App Model Testing**: Quick "Test connection" button verifies API key, endpoint reachability, and model responsiveness with direct visual feedback.

### 4. Audio & Voice Input
- Animated glowing mic indicator for in-app voice input.
- Default home redirect and key filter configuration enabled out-of-the-box.

## Verification
- Validated on TCL Smart_TV_Pro G08_4K (Android 14 / API 34).
- Structure audit passing: 0 raw TV buttons, 14 manifest components, 77 app classes, core Android-free.
- Unit test suite passing across search repository, JSON parsing, and provider configurations.
- Optimized release checks passed: R8 minification, resource shrinking, lint.
- Optimized APK: **1,826,490 bytes (1.74 MiB)**, `versionCode 123`.
- SHA-256: `DE6D51E07EFEFB9ED891A901CB6EAC576397487801D0AA73AFDBA84F40FCA8D6`.

