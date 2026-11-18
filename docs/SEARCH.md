# Optional catalog search

The Home toolbar's Search icon opens a shared search page. Configure services under **Settings → Integrations**. SIMKL account/list settings now live there too. Both search toggles default to Off; connecting SIMKL or saving a TMDB credential does not silently enable search.

- **SIMKL → Use in Search** requires a connected account. It uses the existing AUTH V2 client, refresh handling, shared lock and quota cooldown policy. One submitted query calls `/search/all` for at most 24 mixed movie/TV/anime matches.
- **TMDB → API credential** accepts your API v3 key or API Read Access Token from `themoviedb.org/settings/api`. Then enable **Use in Search**. Multi-search returns movie/TV/person matches; Matrix filters out people and unusable IDs. TMDB does not require a SIMKL account.
- Only enabled, configured providers appear. With both available, choose the provider at the top of the query page. With neither available, Search offers Integrations instead of an unusable query field.

Requests run only on submission, off the main thread. Typing, opening Home, switching providers and visiting settings do not issue search requests. An eight-query memory cache expires after ten minutes and is cleared when search configuration changes; no query history is written to disk. This is a top-matches search, not exhaustive catalog paging. Existing image/cache limits remain unchanged, and no new networking or playback library is included.

The query field, enabled-provider selector and results remain on one Search screen. The input is shared with setup forms; results share the existing list grid, card size, fitted artwork, focus rim and loading state. Search uses the same opaque artwork-color gradient as other wide panels; it is not transparent wallpaper or a static screenshot. One top row contains a compact Back icon, the query field, a submit magnifier and a separate microphone action. The former Home breadcrumb, Search title and second input row are gone; posters begin immediately below. Voice appears only when the TV exposes a compatible recognition activity. A single provider is named in the input hint; multiple providers get a separate compact selector. The stacked tonal panels and separate full-width information strip were removed in beta 50; there is no live blur or extra per-poster shadow.

Each result has a distinct Movie/Series/Anime badge and year/ratings beneath its poster. Available SIMKL status/episode count or a TMDB synopsis excerpt stays with that poster as one short supporting line; longer text is ellipsized. Search cards reserve the same supporting-line height so missing data does not misalign a row. SIMKL requests `extended=full` once, rather than doing a detail lookup for each poster. Unrated or missing values are omitted. Runtime, cast and other detail-only fields are not fetched separately just to populate search cards.

Back first returns to the first result, then focuses the query while keeping results visible; another Back follows actual navigation history. Selecting a title passes its typed identity through the shared media router. In the local 1.1.1-beta.3 preview, **Settings → Integrations → Open titles with** selects Nuvio or Stremio. Stremio uses IMDb IDs, with optional on-click TMDB mapping; native app Continue Watching requests remain with their publisher. See [media app setup and ID limits](MEDIA-APPS.md) and the [Nuvio compatibility route](NUVIO-ROUTING.md). Matrix has no stream resolver or playback engine; media-app accounts/profiles stay independent.

Credentials stay in private app preferences (backup disabled), outside source/build metadata and diagnostic dumps. Search diagnostics report only available/selected provider, result count and busy state, never the query, credential, token or media URL. Do not commit credentials or private device artifacts.

## Text and voice interaction

Voice is a separate microphone action beside the submit magnifier, with an accessible Voice search label. Neither action is nested inside the field. It delegates to Android's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` through the existing Activity Result dependency, with the required free-form language model. The system recognition activity owns microphone access; Matrix adds no recording permission, recognition service, always-listening process or hardware Assistant-key override. Recognition availability is checked with a narrow manifest intent query, and unavailable or security-blocked launches produce a safe text-entry fallback.

An accepted, nonblank result fills the field and uses the same explicit submission, provider, validation and cache path as typed input. Cancellation does not submit or clear the draft/results. IME Search also submits; Down from editing moves to the separate Search action. Remote Back keeps the existing first-result → query → previous-page sequence. Voice recognition quality, microphone routing and online/offline behavior depend on the TV's installed recognizer.

This implements the platform's activity-based recognition contract rather than copying legacy Leanback SearchFragment or adding a SpeechRecognizer lifecycle. The obsolete trailing-action slot and extra field Row were removed, not retained alongside the new controls. Beta 52 also removes the duplicated page header, submitted-query state, request counter and completed-request counter. One composition-scoped cancellable job owns fetching and result focus, including repeated cache hits. Search returns a card list directly instead of constructing an unused row title/ID wrapper. Credentials remain in the repository; each job captures its submitted query so editing the draft does not issue another request or change an in-flight query.

## Data attribution

**About → Data sources** identifies SIMKL and TMDB and includes TMDB's required notice and logo. The TMDB mark is its own brand asset, not Matrix-owned artwork relicensed under MIT. Its vector path, aspect ratio and gradient colors are preserved in the Android drawable.

Official downloaded logo: `https://www.themoviedb.org/assets/2/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg`

Original SVG SHA256: `8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c`.

## Primary references

- [SIMKL text search and mixed result types](https://api.simkl.org/api-reference/simkl/search-by-text)
- [SIMKL search guidance](https://api.simkl.org/guides/search)
- [TMDB multi-search](https://developer.themoviedb.org/reference/search-multi)
- [TMDB application authentication](https://developer.themoviedb.org/docs/authentication-application)
- [TMDB attribution requirements](https://developer.themoviedb.org/docs/faq)
- [TV Material Surface and tonal elevation](https://github.com/androidx/androidx/blob/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/Surface.kt)
- [Nuvio public deep-link parser](https://github.com/NuvioMedia/NuvioTV/blob/dev/app/src/main/java/com/nuvio/tv/core/deeplink/DeepLinkParser.kt)

- [Android speech recognition activity contract](https://developer.android.com/reference/android/speech/RecognizerIntent)

Warm/cold title routing, compatibility limits and exact upstream references: [Nuvio routing](NUVIO-ROUTING.md).

## SIMKL AI search research (2026-11-04)

SIMKL's [website search guide](https://docs.simkl.org/how-to-use-simkl/core-features/search-and-discovery/using-search) describes natural-language/AI suggestions. Its [public API search guide](https://api.simkl.org/guides/search), [text endpoint](https://api.simkl.org/api-reference/simkl/search-by-text) and [complete documentation index](https://api.simkl.org/llms.txt) document title search but no public AI endpoint or AI flag. This is a documentation finding, not proof a private service cannot exist. No undocumented endpoint or HTML scraping was added. The user chose to leave AI mode out until supported; ordinary inline search/voice stays unchanged and is not relabeled AI.
