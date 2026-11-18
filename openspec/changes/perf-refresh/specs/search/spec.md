# Search capability — delta spec

## MODIFIED Requirements

- WHEN a search repeats within 10 minutes, THE SYSTEM SHALL return the
  cached cards without network calls. The cache holds the last 8 searches
  (LRU). Existing behaviour; preserved unchanged.

- THE SYSTEM SHALL bound the web-search cache to 32 entries (LRU, evict
  on write) and cap DuckDuckGo and Wikipedia reads at 512 KB. (D-05)

- WHEN a web search runs, THE SYSTEM SHALL fetch DuckDuckGo and Wikipedia
  in parallel instead of sequentially. (D-05)

## ADDED Requirements

- WHEN the user activates Refresh with a query entered, THE SYSTEM SHALL
  re-fetch that query from the provider, bypassing the caches, and
  replace the cached entry with the fresh results.

- THE force-refresh bypass SHALL be covered by a JVM-only unit test:
  with the 10-minute cache seeded, a second search without force
  refresh returns the cached cards, and with `forceRefresh = true`
  it does not. The test seam SHALL be internal and test-only, with
  no production behaviour change when force refresh is off.

- WHEN force refresh is off, THE SYSTEM SHALL behave exactly as before
  (cached results within the TTL, live fetch after it).

- WHEN a query is entered, THE SYSTEM SHALL show a Refresh button beside
  the Submit button in the search header. WHEN activated, THE SYSTEM
  SHALL re-run the current query with force refresh and focus the first
  result, as a normal search does.

- WHEN an AI search runs with force refresh, THE SYSTEM SHALL bypass the
  web-search cache for that search's web lookups, so the forced search
  is fully fresh.

## Non-goals

- The 30-minute search-suggestions cache is untouched; refresh is about
  results, not suggestions.
- Normal search behaviour, result order and quota-saving caching are
  unchanged when Refresh is not used.
