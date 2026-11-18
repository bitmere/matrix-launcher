# Media apps and universal Android TV channels

Available in stable 1.2.0. No media app, SIMKL account or search provider is required to use Matrix as an app launcher.

## Choose the app for SIMKL and Search

Open **Settings → Integrations → Open titles with**. Choices apply immediately:

- **Automatic**: use compatible installed Nuvio, otherwise Stremio. This preserves existing Nuvio behavior while allowing Stremio-only installations.
- **Nuvio / Stremio**: shown only when that installed package exposes its public title handler. An explicit unavailable selection never silently switches apps; selecting a title explains how to configure it again.

This preference affects Matrix's catalog cards, including SIMKL Home/library/custom lists and both search providers. App icons still resume apps normally. Account/profile/add-on setup stays in each media app; Matrix never reads another app's private settings or copies its credentials.

Stremio detail links use its documented movie/series route with autoPlay=false. Its common Cinemeta IDs are IMDb IDs. Direct IMDb identities need no lookup. SIMKL cards preserve their original SIMKL ID and media endpoint; if IMDb is missing, selecting a card fetches that one SIMKL detail record through the existing account/auth/quota policy and caches only its IMDb mapping in the existing bounded snapshot store. No separate TMDB key is required for that path. Search results deliberately do not fetch one detail per poster.

TMDB-only search cards can map to IMDb through the user's configured TMDB credential, only on selection, with up to 128 public mappings in memory. Missing mappings and unsupported identities show an explanation rather than guessing another title. Metadata coverage and installed add-ons can still limit what Stremio opens; arbitrary add-on ID namespaces are not assumed equivalent.

Stremio 1.12.1 uses a standard exported Activity: CLEAR_TOP recreates it so it reads the new URI. Do not add SINGLE_TOP: the tested build otherwise reopens its Home instead of the requested title. The standalone official-URI test verified The Matrix with CLEAR_TOP and no autoplay; no task clearing or force-stop is used.

Nuvio retains the beta-10-tested fresh detail handoff described in [Nuvio routing](NUVIO-ROUTING.md). Future app versions still require real title-switching tests.

## Continue Watching and channels from any app

Choose source apps and rows in **Settings → Home channels**, allow TV listings, and enable Home/channel sharing inside each source app when it offers that setting. Matrix reads Android's published PreviewPrograms and WatchNextPrograms. A card returns to its original publishing package using the published intent, episode/progress context and routing flags. Choosing Stremio for Search does not redirect a Nuvio Continue Watching entry into Stremio, or vice versa.

Only rows actually published and readable on that TV can appear. Installing an app or choosing it as the title target does not create its Continue Watching feed. Reload Home rereads published rows; it cannot force a source app to sync, expose private watch history or support an absent deep-link contract. Existing Nuvio queue filtering remains a narrow provider compatibility rule, not a requirement on other apps.

## Architecture and extending support

- Android-free **MediaLinks** owns validated movie/series identities in the internal matrix-title scheme and the bounded Stremio link builder. Catalog providers produce matrix.catalog items; they no longer declare Nuvio as the destination. Legacy Nuvio metadata parsing is retained for native publisher requests.
- **MatrixMediaRouter** is the only media intent adapter. Its installed-app registry owns package IDs, public handler probes, availability and target selection. The Nuvio-specific workaround stays isolated here; Search, lists and Home share it.
- Native publisher intents stay package-scoped; foreign components, selectors, clip data, permission grants and task-clearing flags are rejected/removed. Catalog detail requests are newly constructed and cannot inherit stream extras.
- On-click resolution runs on IO. A newer click, page/Home/Back navigation, disposal or launcher stop cancels pending delivery. A result cannot open an app after Matrix loses its resumed state.

To add a future media app, document its exported title-link contract and supported ID namespace, add one registry/adapter entry and narrow package visibility, then test missing-app behavior, ID compatibility, cold/warm movie/series switching, no unintended autoplay and return navigation. Do not add per-provider copies of launch logic or redirect an app's private resume request into a different app. There is no Android-wide universal title-ID-to-player contract; Android TV's standardized channel contract is the portable integration for app-owned content.

## Official references

- [Android TV channels, programs and published intent URIs](https://developer.android.com/training/tv/discovery/recommendations-channel)
- [Watch Next program contract](https://developer.android.com/reference/android/media/tv/TvContract.WatchNextPrograms)
- [Stremio official deep-link routes and platform caveats](https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/deep-links.md)
- [SIMKL Search result identifiers](https://api.simkl.org/api-reference/simkl/search-by-text)
- [SIMKL detail endpoint](https://api.simkl.org/api-reference/simkl/get-movie)
- [TMDB movie external IDs](https://developer.themoviedb.org/reference/movie-external-ids)
- [TMDB TV external IDs](https://developer.themoviedb.org/reference/tv-series-external-ids)

## Validation

Beta 1 verified selection/missing-credential handling but exposed SIMKL Search's limited ID set. Beta 2 adds SIMKL reference/detail resolution; direct Stremio URI testing then exposed SINGLE_TOP reuse. Beta 3 uses the device-verified CLEAR_TOP recreation contract. Final beta 3 (code 88) installed and hash-verified. Actual Matrix Search cards opened The Batman then Batman: The Animated Series in Stremio 1.12.1, including the missing-IMDb SIMKL lookup with no TMDB credential. A SIMKL Home-list card opened Aadarsha Kutumbam House No: 47 in Stremio. Selecting Nuvio then opened the expected movie and series in Nuvio 1.1.0-beta.10. Dumps guarded page/focus/preference; selective screenshots verified actual destination titles. A fixed Down-count assumption stopped safely and was replaced by actual row-index checks. The screensaver also caused a guarded stop; the TV was woken before retrying. No Play, episode/stream selection, task clearing or profile change in these final tests. Automatic was restored (Nuvio on this TV), and automatic input stopped on Matrix Home. All 139 JVM tests pass; lint has no errors and the existing orientation warning. Full native Continue Watching playback and live TMDB credential mapping were not repeated. A Stremio-only installation is covered by selection-policy tests; Nuvio was not removed from the test TV. Missing-app/automatic/explicit choice policy, neutral ID parsing, unsupported identities and TMDB mapping responses have JVM coverage. A passing unit suite does not prove an installed app's warm routing or its private watch history publication. No release publication is authorized for this preview.
