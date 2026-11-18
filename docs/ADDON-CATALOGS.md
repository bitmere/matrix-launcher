# Add-on catalogs — feasibility and proposed integration

Researched 2026-11-04. This feature is not implemented in the current build.

Matrix currently reads Android TV channels published by apps and optional SIMKL data. Nuvio's inspected AndroidTvChannelSyncService publishes Continue Watching; this does not expose all configured add-on catalogs to launchers. To show those directly, Matrix needs its own small read-only Stremio catalog adapter, using the user's configured manifest URL.

| Source | Candidate support | Boundary |
| --- | --- | --- |
| BingeCat | User-published catalog lists | Requires the configured manifest; provider account UI remains external. |
| AIOMetadata | Selected movie, series and anime catalogs | Preserve metadata IDs and poster shape; check title-link compatibility. |
| Xperience | Standard manifest catalog rows | Nuvio collections, folders, badges and animations are separate extensions. Standard catalog support does not reproduce its whole Nuvio layout. |
| AIOStreams | Catalogs exposed by the configured manifest | Stream-only resources cannot become browsing rows. No stream resolution/player is needed inside Matrix. |

## Reuse and runtime behavior

- One protocol adapter, not a different client per brand. Read `manifest.catalogs`, then `/catalog/{type}/{id}.json`, respecting declared required extras and optional pagination. Search-only catalogs need a search value and must not be fetched as ordinary blank Home rows.
- Reuse `MediaRow`/`MediaCard`, shared artwork controls, Home ordering/visibility, list sizes, list navigation and the existing image loader. Add source-specific stable IDs without putting configured URLs/tokens into diagnostic IDs.
- Add/import a configured manifest, choose catalogs, select their order and open titles in Nuvio. Keep SIMKL and app-published channels independent and optional. Do not read another app's private preferences or silently alter its installed add-ons.
- Show bounded cached previews immediately. Fetch only enabled catalogs; bound concurrent network work and response sizes, respect provider caching, and keep failures local to the affected source. No permanent polling service or embedded add-on runtime.
- Nuvio's public `nuvio://meta/{type}/{id}` route can open details. The current Matrix link helper accepts known IMDb/TMDB/MAL/SIMKL IDs only; add-on-specific IDs and anime variants need reviewed mapping/support rather than being silently treated as IMDb titles. Nuvio may need the same metadata add-on installed to resolve those IDs. Verify exact configured manifests before declaring brand compatibility.
- Optional `See all` uses the existing list screen with provider paging where supported. Standard catalog fetching does not guarantee live Continue Watching, account/profile sync, or immediate playback.

## Primary references

- [Stremio add-on protocol](https://stremio.github.io/stremio-addon-sdk/protocol.html)
- [Manifest and catalogs](https://stremio.github.io/stremio-addon-sdk/api/responses/manifest.html)
- [Catalog extras and paging](https://stremio.github.io/stremio-addon-sdk/api/requests/defineCatalogHandler.html)
- [BingeCat](https://bingecat.com/?ui=legacy)
- [AIOMetadata](https://github.com/cedya77/aiometadata)
- [AIOStreams](https://github.com/Viren070/AIOStreams)
- [Xperience guide](https://xperience-app.com/guide)
- [Nuvio public deep-link parser](https://github.com/NuvioMedia/NuvioTV/blob/dev/app/src/main/java/com/nuvio/tv/core/deeplink/DeepLinkParser.kt)
- [Nuvio channel publisher](https://github.com/NuvioMedia/NuvioTV/blob/dev/app/src/main/java/com/nuvio/tv/core/sync/androidtv/AndroidTvChannelSyncService.kt)
