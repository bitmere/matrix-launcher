package com.matrix.launcher

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import com.matrix.launcher.core.MediaLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class CatalogUnavailableException(cause: Throwable? = null) : Exception("Catalog unavailable", cause)

internal fun combineCatalogResults(
    movies: Result<List<MediaCard>>,
    series: Result<List<MediaCard>>,
    query: String
): Result<List<MediaCard>> {
    val movieFailure = movies.exceptionOrNull()
    if (movieFailure != null && series.isFailure) {
        return Result.failure(movieFailure)
    }
    return Result.success(
        CinemetaClient.rankResults(movies.getOrDefault(emptyList()), series.getOrDefault(emptyList()), query)
    )
}

/** Compiled once: these patterns sit on search hot paths (D-04). Constructing a
 * Regex compiles the pattern, so building one per call cost thousands of
 * compilations per AI search. */
private val WHITESPACE_REGEX = Regex("\\s+")
private val DIGITS_ONLY_REGEX = Regex("[0-9]+")
private val TMDB_V3_KEY_REGEX = Regex("[a-fA-F0-9]{32}")
private val TMDB_POSTER_PATH_REGEX = Regex("/[A-Za-z0-9._/-]+")
private val IMDB_ID_REGEX = Regex("tt[0-9]+")
private val CLEAN_TITLE_YEAR_REGEX = Regex("""\(\d{4}\)""")
private val CLEAN_TITLE_ARTICLE_REGEX = Regex("""\b(the|a|an)\b""")
private val CLEAN_TITLE_NON_ALNUM_REGEX = Regex("""[^\p{L}\p{N}]""")

internal enum class MatrixSearchProvider(val title: String, val preference: String) {
    CATALOG("Catalog", "search_catalog_enabled"),
    SIMKL("SIMKL", "search_simkl_enabled"),
    TMDB("TMDB", "search_tmdb_enabled"),
    AI("AI Discovery", "ai_discovery_enabled")
}

/**
 * Live data fetchDynamicAiSuggestions() grounds itself in. Production uses
 * the real Cinemeta + web-search clients; tests pass a stub so the
 * suggestion builder runs with no network. Defaulted so existing call
 * sites are untouched.
 */
internal interface DynamicSuggestionsSource {
    fun topMetas(type: String): List<org.json.JSONObject>
    suspend fun webSearch(query: String, maxResults: Int): List<WebSnippet>
}

internal object LiveDynamicSuggestionsSource : DynamicSuggestionsSource {
    override fun topMetas(type: String) = CinemetaClient.getTopMetas(type, 2)
    override suspend fun webSearch(query: String, maxResults: Int) =
        MatrixWebSearchEngine.search(query, maxResults = maxResults)
}

/** Explicit searches only. Shares SIMKL auth/quota/lock; no second client or background worker. */
internal class MatrixSearchRepository(private val prefs: SharedPreferences, private val simkl: SimklClient? = null,
    private val simklGate: Mutex = Mutex(), private val context: android.content.Context? = null,
    private val suggestionsSource: DynamicSuggestionsSource = LiveDynamicSuggestionsSource,
    private val catalogSearch: (String, String) -> Result<List<MediaCard>> = CinemetaClient::searchCatalog) {
    val ai = MatrixAiClient(prefs, context).apply {
        onConfigurationChanged = { clearCache() }
    }
    private data class Cached(val at: Long, val cards: List<MediaCard>)
    private val cache = object : LinkedHashMap<String, Cached>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>?) = size > 8
    }
    private val identities = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 128
    }
    private val suggestionsLock = Any()
    var lastAiSummary: String = ""
    var tmdbKey: String
        get() = prefs.getString("tmdb_api_key", "").orEmpty()
        set(value) { prefs.edit { putString("tmdb_api_key", value.trim()) }; clearCache() }
    fun enabled(provider: MatrixSearchProvider) = when (provider) {
        MatrixSearchProvider.CATALOG -> prefs.getBoolean(provider.preference, true)
        else -> prefs.getBoolean(provider.preference, false)
    }
    fun enable(provider: MatrixSearchProvider, value: Boolean) {
        prefs.edit { putBoolean(provider.preference, value) }; clearCache()
    }
    fun ready(provider: MatrixSearchProvider) = when (provider) {
        MatrixSearchProvider.CATALOG -> true
        MatrixSearchProvider.SIMKL -> simkl?.connected() == true
        MatrixSearchProvider.TMDB -> tmdbKey.isNotBlank()
        MatrixSearchProvider.AI -> ai.ready
    }
    fun providers() = MatrixSearchProvider.entries.filter { enabled(it) && ready(it) }
    fun clearCache() {
        synchronized(cache) { cache.clear() }
        synchronized(identities) { identities.clear() }
    }

    fun recentSearches(): List<String> = runCatching {
        val raw = prefs.getString("search_recent_queries", "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val current = recentSearches().filterNot { it.equals(clean, ignoreCase = true) }.toMutableList()
        current.add(0, clean)
        val limited = current.take(10)
        val arr = org.json.JSONArray(limited)
        prefs.edit { putString("search_recent_queries", arr.toString()) }
    }

    fun clearRecentSearches() {
        prefs.edit { remove("search_recent_queries") }
    }

    private var dynamicSuggestionsCache: Pair<Long, List<String>>? = null

    suspend fun fetchDynamicAiSuggestions(): List<String> = withContext(Dispatchers.IO) {
        val cached = synchronized(suggestionsLock) { dynamicSuggestionsCache }
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.first) < 30 * 60 * 1000L && cached.second.isNotEmpty()) {
            return@withContext cached.second
        }

        val suggestions = LinkedHashSet<String>()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        // 1. Fetch live top trending movie titles and genres from Cinemeta
        runCatching {
            val topMovies = suggestionsSource.topMetas("movie")
            if (topMovies.isNotEmpty()) {
                val top0 = topMovies[0]
                val name0 = top0.optString("name").trim()
                if (name0.isNotBlank()) suggestions.add("Movies like $name0")

                val topGenres = top0.optJSONArray("genres")
                val genre0 = topGenres?.optString(0)?.trim()
                if (!genre0.isNullOrBlank()) suggestions.add("Top $genre0 movies $currentYear")

                if (topMovies.size > 1) {
                    val name1 = topMovies[1].optString("name").trim()
                    if (name1.isNotBlank()) suggestions.add("Similar to $name1")
                }
            }
        }

        // 2. Fetch live top trending series from Cinemeta
        runCatching {
            val topSeries = suggestionsSource.topMetas("series")
            if (topSeries.isNotEmpty()) {
                val top0 = topSeries[0].optString("name").trim()
                if (top0.isNotBlank()) suggestions.add("Binge-worthy series like $top0")
            }
        }

        // 3. Web Search Engine grounding for fresh current-year trending titles
        runCatching {
            val snippets = suggestionsSource.webSearch("top trending new movies $currentYear", maxResults = 3)
            for (snip in snippets) {
                val clean = snip.title.substringBefore(" - ").substringBefore(" | ").substringBefore(" (").trim()
                if (clean.length in 3..40 && !suggestions.any { it.contains(clean, ignoreCase = true) }) {
                    suggestions.add("$clean and similar titles")
                    break
                }
            }
        }

        // 4. Fallback defaults if completely offline or empty
        if (suggestions.isEmpty()) {
            suggestions.add("Trending movies $currentYear")
            suggestions.add("Mind-bending sci-fi thrillers")
            suggestions.add("Award-winning drama series")
            suggestions.add("Top action comedy releases")
        }

        val result = suggestions.toList().take(6)
        synchronized(suggestionsLock) { dynamicSuggestionsCache = Pair(now, result) }
        result
    }

    /**
     * Cache key for a search: provider + normalized query. AI keys also
     * carry the provider/model/web-search/result-count, so a config change
     * misses the old entry. Extracted from search() so tests can build the
     * same key a real search would.
     */
    internal fun searchCacheKey(provider: MatrixSearchProvider, rawQuery: String): String {
        val query = rawQuery.trim().replace(WHITESPACE_REGEX, " ")
        return if (provider == MatrixSearchProvider.AI) {
            "AI:${ai.provider.name}:${ai.activeModel}:${ai.webSearch}:${ai.resultCount}:${query.lowercase(java.util.Locale.ROOT)}"
        } else {
            provider.name + ":" + query.lowercase(java.util.Locale.ROOT)
        }
    }

    /**
     * The exact cache-read decision search() uses: with [forceRefresh] the
     * entry is skipped (a later write still overwrites it); without it, a
     * fresh entry (at most 10 minutes old) is returned. Internal so the
     * force-refresh bypass test can assert the decision directly.
     */
    internal fun cachedSearchCards(key: String, forceRefresh: Boolean): List<MediaCard>? {
        if (forceRefresh) return null
        val now = System.currentTimeMillis()
        return synchronized(cache) { cache[key]?.takeIf { now - it.at in 0..600_000 } }?.cards
    }

    /**
     * Test-only seam: seeds the 10-minute search cache exactly as a
     * completed search would (same key, same entry shape). Production code
     * never calls this. [at] defaults to now; a test may pass an older
     * timestamp to simulate a stale entry.
     */
    internal fun seedSearchCacheForTest(provider: MatrixSearchProvider, rawQuery: String, cards: List<MediaCard>, at: Long = System.currentTimeMillis()) {
        synchronized(cache) { cache[searchCacheKey(provider, rawQuery)] = Cached(at, cards) }
    }

    /**
     * Executes a media search across the selected provider (Catalog, SIMKL, TMDB, or AI Discovery).
     * Results are cached for 10 minutes to minimize network and API quota consumption.
     * With [forceRefresh] the cached entry for this query is skipped and overwritten
     * with fresh results; nothing else changes.
     */
    suspend fun search(provider: MatrixSearchProvider, rawQuery: String, onProgress: ((String) -> Unit)? = null, forceRefresh: Boolean = false): List<MediaCard> = withContext(Dispatchers.IO) {
        require(provider in providers()) { "Enable and configure this provider in Settings → Integrations." }
        val query = rawQuery.trim().replace(WHITESPACE_REGEX, " ")
        require(query.length in 2..120) { "Enter 2–120 characters." }
        val key = searchCacheKey(provider, rawQuery)
        val now = System.currentTimeMillis()
        val saved = cachedSearchCards(key, forceRefresh)
        saved ?: when (provider) {
            MatrixSearchProvider.CATALOG -> cinemetaSearch(query, onProgress).getOrElse {
                throw CatalogUnavailableException(it)
            }
            MatrixSearchProvider.SIMKL -> simklGate.withLock { simkl?.search(query)?.items?.map { item ->
                MediaCard(item.intent, item.title.orEmpty(), item.poster.orEmpty(), item.intent,
                    MediaLinks.SOURCE, true, 0, 0, item.metadata.orEmpty(), item.mediaType.orEmpty(), item.summary.orEmpty(), item.simklReference.orEmpty())
            } ?: emptyList() }
            MatrixSearchProvider.TMDB -> tmdb(query)
            MatrixSearchProvider.AI -> searchAi(query, onProgress, forceRefresh)
        }.distinctBy { it.id }.also {
            if (it.isNotEmpty()) {
                addRecentSearch(query)
                synchronized(cache) { cache[key] = Cached(now, it) }
            }
        }
    }

    /**
     * Searches both movie and TV series catalogs on Cinemeta in parallel,
     * merging and ranking results based on query relevance and media popularity.
     */
    private suspend fun cinemetaSearch(query: String, onProgress: ((String) -> Unit)? = null): Result<List<MediaCard>> = coroutineScope {
        onProgress?.invoke("Searching Cinemeta catalog…")
        val movieDeferred = async(Dispatchers.IO) { catalogSearch("movie", query) }
        val seriesDeferred = async(Dispatchers.IO) { catalogSearch("series", query) }
        combineCatalogResults(movieDeferred.await(), seriesDeferred.await(), query)
    }

    /**
     * Groups AI recommendations by what their resolution depends on (D-02):
     * identical suggestions (same cleaned title, type, year and IMDb id) land
     * in one group and resolve once; the same title with a different year or
     * IMDb id (a remake, a corrected id) is a separate group. Pure function
     * (groupBy preserves first-appearance order), extracted so tests can
     * assert the grouping without a network fetch.
     */
    internal fun groupRecommendationsForResolution(recommendations: List<AiRecommendation>): Map<ResolutionKey, List<AiRecommendation>> =
        recommendations.groupBy { ResolutionKey(cleanTitle(it.title), it.type, it.year, it.imdbId) }

    /**
     * Executes AI natural-language discovery and resolves high-resolution posters, verified release
     * years, and IMDb/TMDB/SIMKL ratings in parallel with a bounded concurrency throttle.
     */
    private suspend fun searchAi(query: String, onProgress: ((String) -> Unit)? = null, forceRefresh: Boolean = false): List<MediaCard> = coroutineScope {
        lastAiSummary = ""
        onProgress?.invoke("Consulting ${ai.activeModel} (${ai.provider.label})…")
        val recommendations = ai.discover(query, onProgress, forceRefresh)
        lastAiSummary = ai.lastSummary
        val total = recommendations.size
        onProgress?.invoke("Resolving posters for $total recommendations…")
        val throttle = kotlinx.coroutines.sync.Semaphore(12)
        var resolvedCount = 0
        // Per-search memos (D-02): repeated titles share lookups within this one
        // search; the memos die with the call, so nothing goes stale.
        val tmdbMemo = BoundedCache<String, List<MediaCard>>(64)
        val catalogMemo = BoundedCache<Pair<String, String>, List<MediaCard>>(64)
        val metaMemo = BoundedCache<Pair<String, String>, JSONObject?>(64)
        // Dedupe (D-02): identical suggestions resolve once. Every suggestion
        // still gets its own card, built from the shared resolution with its
        // own reason and year, so the returned titles and order are unchanged.
        val groups = groupRecommendationsForResolution(recommendations)
        val resolvedByKey = mutableMapOf<ResolutionKey, MediaCard?>()
        groups.map { (key, group) ->
            async(Dispatchers.IO) {
                throttle.withPermit {
                    val resolved = resolveRecommendation(group.first(), tmdbMemo, catalogMemo, metaMemo)
                    synchronized(this@coroutineScope) {
                        resolvedCount += group.size
                        if (total >= 20 && (resolvedCount % 5 == 0 || resolvedCount == total)) {
                            onProgress?.invoke("Resolving posters ($resolvedCount/$total titles)…")
                        }
                    }
                    synchronized(resolvedByKey) { resolvedByKey[key] = resolved }
                }
            }
        }.awaitAll()
        val currentCalYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        recommendations.map { rec ->
            buildAiCard(rec, resolvedByKey[ResolutionKey(cleanTitle(rec.title), rec.type, rec.year, rec.imdbId)], currentCalYear)
        }
    }

    /** Resolves one recommendation to its poster card (D-02). Called once per
     * unique [ResolutionKey]; the memos share TMDB/Cinemeta lookups between
     * groups within the same search. */
    private suspend fun resolveRecommendation(
        rec: AiRecommendation,
        tmdbMemo: BoundedCache<String, List<MediaCard>>,
        catalogMemo: BoundedCache<Pair<String, String>, List<MediaCard>>,
        metaMemo: BoundedCache<Pair<String, String>, JSONObject?>
    ): MediaCard? {
        val titleQuery = rec.title
        val targetClean = cleanTitle(rec.title)
        return if (tmdbKey.isNotBlank()) {
            val tmdbMatches = memoLookup(tmdbMemo, titleQuery) { runCatching { tmdb(titleQuery) }.getOrDefault(emptyList()) }
            val typedMatches = tmdbMatches.filter { mediaKind(it.mediaType) == rec.type }
            val matchedTmdb = matchRecommendation(rec, typedMatches)
            matchedTmdb ?: resolveCinemeta(rec, catalogMemo, metaMemo)
        } else if (simkl?.connected() == true) {
            runCatching {
                simklGate.withLock {
                    val items = simkl?.search(titleQuery)?.items.orEmpty().filter { mediaKind(it.mediaType) == rec.type }
                    val tier1 = items.firstOrNull { item ->
                        cleanTitle(item.title.orEmpty()) == targetClean && matchesYear(rec.year, item.metadata.orEmpty().substringBefore("\n"), tolerance = 0)
                    }
                    val tier2 = items.firstOrNull { item ->
                        cleanTitle(item.title.orEmpty()) == targetClean && matchesYear(rec.year, item.metadata.orEmpty().substringBefore("\n"), tolerance = 1)
                    }
                    val tier3 = items.firstOrNull { item ->
                        rec.year.isBlank() && cleanTitle(item.title.orEmpty()) == targetClean
                    }
                    val matchedItem = tier1 ?: tier2 ?: tier3
                    matchedItem?.let { item ->
                        MediaCard(item.intent, item.title.orEmpty(), item.poster.orEmpty(), item.intent,
                            MediaLinks.SOURCE, true, 0, 0, item.metadata.orEmpty(), item.mediaType.orEmpty(), rec.reason, item.simklReference.orEmpty())
                    }
                }
            }.getOrNull() ?: resolveCinemeta(rec, catalogMemo, metaMemo)
        } else {
            resolveCinemeta(rec, catalogMemo, metaMemo)
        }
    }

    /** Builds one AI card from a shared resolution and this recommendation's own
     * reason and year (D-02). The resolution is shared; the card is not. */
    private fun buildAiCard(rec: AiRecommendation, resolvedCard: MediaCard?, currentCalYear: Int): MediaCard {
        if (resolvedCard != null) {
            val rawYear = rec.year.ifBlank { resolvedCard.metadata.substringBefore("\n").substringBefore(" · ").trim() }
            val year = rawYear.filter(Char::isDigit).take(4).ifBlank { rawYear }
            val ratingBadge = extractRatingBadge(resolvedCard.metadata)
            val ratingText = when {
                ratingBadge == null -> null
                resolvedCard.metadata.contains("IMDb", ignoreCase = true) -> "IMDb " + ratingBadge.removePrefix("★ ")
                resolvedCard.metadata.contains("TMDB", ignoreCase = true) -> "TMDB " + ratingBadge.removePrefix("★ ")
                resolvedCard.metadata.contains("SIMKL", ignoreCase = true) -> "SIMKL " + ratingBadge.removePrefix("★ ")
                else -> ratingBadge.removePrefix("★ ")
            }
            val finalMetadata = when {
                ratingText != null -> (year.ifBlank { "Movie" } + " · " + ratingText).trim()
                year.toIntOrNull() != null && year.toInt() >= currentCalYear -> "$year · Upcoming"
                else -> (year.ifBlank { "Movie" } + " · " + rec.type.replaceFirstChar { it.uppercase() }).trim()
            }
            return resolvedCard.copy(
                metadata = finalMetadata,
                mediaType = "AI Pick",
                summary = "Why: " + rec.reason.ifBlank { resolvedCard.summary }
            )
        } else {
            val link = if (rec.imdbId.isNotBlank()) MediaLinks.title(rec.type, rec.imdbId) else MediaLinks.title(rec.type, "tmdb:0")
            val year = rec.year.filter(Char::isDigit).take(4).ifBlank { rec.year.ifBlank { "Movie" } }
            val metaStatus = when {
                year.toIntOrNull() != null && year.toInt() >= currentCalYear -> "$year · Upcoming"
                else -> "$year · " + rec.type.replaceFirstChar { it.uppercase() }
            }
            return MediaCard(
                id = "ai:${rec.title}",
                title = rec.title,
                image = "",
                link = link,
                source = MediaLinks.SOURCE,
                portrait = true,
                position = 0L,
                duration = 0L,
                metadata = metaStatus,
                mediaType = "AI Pick",
                summary = "Why: ${rec.reason}"
            )
        }
    }

    /** Per-search memo (D-02): the first call fetches, repeats reuse the result.
     * Synchronized — group tasks resolve concurrently. A cached null still
     * counts as a hit, so a missing meta is not re-fetched for the same key.
     * Concurrent duplicates may both fetch (last write wins, same result);
     * the groupBy dedupe is what guarantees one fetch per repeated title. */
    private fun <K, V> memoLookup(memo: BoundedCache<K, V>, key: K, fetch: () -> V): V {
        synchronized(memo) {
            if (memo.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                return memo[key] as V
            }
        }
        val result = fetch()
        synchronized(memo) { memo[key] = result }
        return result
    }

    /** Everything one recommendation's resolution depends on (D-02). Two
     * suggestions with the same key are the same title twice and resolve once;
     * the same title with a different year or IMDb id (a remake, a corrected
     * id) is a different key and still resolves independently, so the returned
     * cards are identical to before the change. */
    internal data class ResolutionKey(val title: String, val type: String, val year: String, val imdbId: String)

    private fun resolveCinemeta(
        rec: AiRecommendation,
        catalogMemo: BoundedCache<Pair<String, String>, List<MediaCard>>,
        metaMemo: BoundedCache<Pair<String, String>, JSONObject?>
    ): MediaCard? = runCatching {
        val type = if (rec.type == "series") "series" else "movie"
        var validId = rec.imdbId.takeIf { it.startsWith("tt") }.orEmpty()
        var poster = ""
        var title = rec.title
        var year = rec.year
        var imdbRating = ""
        val targetClean = cleanTitle(rec.title)

        // 1. If validId is present, verify that its title and release year match rec
        if (validId.isNotBlank()) {
            val meta = memoLookup(metaMemo, type to validId) { CinemetaClient.getMeta(type, validId) }
            if (meta != null) {
                val metaTitle = meta.optString("name", "")
                val metaYear = meta.optString("releaseInfo", "")
                val metaClean = cleanTitle(metaTitle)
                val titleMatches = metaTitle.isBlank() || metaClean == targetClean ||
                        metaClean.startsWith(targetClean) || targetClean.startsWith(metaClean)
                val yearMatches = rec.year.isBlank() || metaYear.isBlank() || matchesYear(rec.year, metaYear, tolerance = 1)

                if (!titleMatches || !yearMatches) {
                    validId = ""
                } else {
                    title = meta.optString("name", rec.title)
                    poster = meta.optString("poster", "")
                    if (year.isBlank()) year = metaYear.filter(Char::isDigit).take(4)
                    val r = meta.optString("imdbRating", "").trim()
                    if (r.isNotBlank()) imdbRating = r
                }
            } else {
                validId = ""
            }
        }

        // 2. If no valid ID (or mismatched), search Cinemeta catalog with tiered matching
        if (poster.isBlank() || validId.isBlank()) {
            val cards = memoLookup(catalogMemo, type to rec.title) {
                CinemetaClient.searchCatalog(type, rec.title).getOrDefault(emptyList())
            }
            val tier1 = cards.firstOrNull {
                cleanTitle(it.title) == targetClean && matchesYear(rec.year, it.metadata, tolerance = 0)
            }
            val tier2 = cards.firstOrNull {
                cleanTitle(it.title) == targetClean && matchesYear(rec.year, it.metadata, tolerance = 1)
            }
            val tier3 = cards.firstOrNull {
                rec.year.isBlank() && cleanTitle(it.title) == targetClean
            }
            val tier4 = cards.firstOrNull {
                val candClean = cleanTitle(it.title)
                candClean.startsWith(targetClean) && matchesYear(rec.year, it.metadata, tolerance = 0)
            }
            val match = tier1 ?: tier2 ?: tier3 ?: tier4
            if (match != null) {
                if (validId.isBlank()) validId = match.id.removePrefix("cinemeta:").takeIf { it.startsWith("tt") }.orEmpty()
                if (poster.isBlank()) poster = match.image
                if (cleanTitle(match.title) == targetClean) title = match.title
                if (year.isBlank()) year = match.metadata.filter(Char::isDigit).take(4)
                val r = match.metadata.substringAfter("IMDb ", "").trim()
                if (imdbRating.isBlank() && r.isNotBlank()) imdbRating = r
            }
        }

        // 3. If validId was found from catalog search but the poster or the year
        // is still missing, fetch detail meta. A missing IMDb rating alone does
        // not justify another round-trip: the card is built without it, and the
        // SIMKL cross-reference below may still supply a rating.
        if (validId.isNotBlank() && (poster.isBlank() || year.isBlank())) {
            val meta = memoLookup(metaMemo, type to validId) { CinemetaClient.getMeta(type, validId) }
            if (meta != null) {
                val r = meta.optString("imdbRating", "").trim()
                if (imdbRating.isBlank() && r.isNotBlank()) imdbRating = r
                if (poster.isBlank()) poster = meta.optString("poster", "")
                if (year.isBlank()) year = meta.optString("releaseInfo", "").filter(Char::isDigit).take(4)
            }
        }

        if (year.isBlank()) year = rec.year.filter(Char::isDigit).take(4)

        val link = if (validId.isNotBlank()) MediaLinks.title(type, validId) else ""
        if (link.isBlank()) return@runCatching null

        // Cross-reference SIMKL for additional metadata, rating, and tracking reference
        var simklRef = ""
        var simklRatingText = ""
        val clientId = simkl?.clientId()?.trim().orEmpty()
        if (clientId.isNotBlank() && validId.startsWith("tt")) {
            runCatching {
                val sUrl = "https://api.simkl.com/search/id?imdb=$validId&client_id=$clientId"
                val sConn = PersonalImages.connect(sUrl, null)
                try {
                    if (sConn.responseCode == 200) {
                        val sText = sConn.inputStream.use { PersonalImages.text(it, 256 * 1024) }
                        val sArray = org.json.JSONArray(sText)
                        if (sArray.length() > 0) {
                            val sObj = sArray.getJSONObject(0)
                            val sIds = sObj.optJSONObject("ids")
                            val sId = sIds?.optLong("simkl", 0) ?: 0L
                            if (sId > 0) simklRef = "simkl:$sId"
                            val sRatings = sObj.optJSONObject("ratings")
                            val sImdb = sRatings?.optJSONObject("imdb")
                            val imdbVal = sImdb?.optDouble("rating", 0.0) ?: 0.0
                            if (imdbRating.isBlank() && imdbVal > 0.0) {
                                imdbRating = String.format(java.util.Locale.ROOT, "%.1f", imdbVal)
                            }
                            val sSimkl = sRatings?.optJSONObject("simkl")
                            val rating = sSimkl?.optDouble("rating", 0.0) ?: 0.0
                            if (rating > 0.0) {
                                simklRatingText = String.format(java.util.Locale.ROOT, " · SIMKL %.1f", rating)
                            }
                            if (poster.isBlank()) {
                                val sPoster = sObj.optString("poster", "")
                                if (sPoster.isNotBlank()) poster = "https://simkl.in/posters/${sPoster}_m.jpg"
                            }
                        }
                    }
                } finally { sConn.disconnect() }
            }
        }

        val ratingSuffix = if (imdbRating.isNotBlank()) " · IMDb $imdbRating" else simklRatingText
        MediaCard(
            id = "ai:$validId",
            title = title,
            image = poster,
            link = link,
            source = MediaLinks.SOURCE,
            portrait = true,
            position = 0L,
            duration = 0L,
            metadata = (year.ifBlank { "Movie" } + ratingSuffix).trim(),
            mediaType = "AI Pick",
            summary = "Why: " + rec.reason,
            simklReference = simklRef
        )
    }.getOrNull()

    suspend fun testTmdbConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (tmdbKey.isBlank()) return@withContext Result.failure(IllegalStateException("No TMDB credential set. Enter API key in settings."))
        try {
            val response = tmdbRequest("https://api.themoviedb.org/3/configuration".toUri().buildUpon())
            val images = response.optJSONObject("images")
            if (images != null) {
                Result.success("Success: TMDB API credential verified")
            } else {
                Result.failure(IllegalStateException("Unexpected response from TMDB configuration"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("TMDB test failed: ${e.message ?: e.javaClass.simpleName}"))
        }
    }
    private fun tmdb(query: String): List<MediaCard> = tmdbCards(tmdbRequest("https://api.themoviedb.org/3/search/multi".toUri().buildUpon()
            .appendQueryParameter("query", query).appendQueryParameter("include_adult", "false")))

    /** Only invoked on a Stremio click for a TMDB-only title, never for every result/poster. */
    fun imdbId(type: String, id: String): String {
        require(type in listOf("movie", "series") && id.matches(DIGITS_ONLY_REGEX))
        require(tmdbKey.isNotBlank()) { "This title needs a TMDB-to-IMDb mapping. Add your TMDB credential in Settings → Integrations → TMDB, or choose Nuvio." }
        val key = "$type:$id"
        synchronized(identities) { identities[key] }?.let { return it }
        val kind = if (type == "movie") "movie" else "tv"
        val imdb = requireNotNull(tmdbImdbId(tmdbRequest("https://api.themoviedb.org/3/$kind/$id/external_ids".toUri().buildUpon()))) { "No IMDb mapping is available for this title in Stremio." }
        synchronized(identities) { identities[key] = imdb }
        return imdb
    }
    private fun tmdbRequest(url: android.net.Uri.Builder): JSONObject {
        val credential = tmdbKey
        // Accept either the v3 API key or the API Read Access Token; never log either.
        val v3 = credential.matches(TMDB_V3_KEY_REGEX)
        if (v3) url.appendQueryParameter("api_key", credential)
        val connection = PersonalImages.connect(url.build().toString(), if (v3) null else credential)
        try {
            val code = connection.responseCode
            check(code == 200) { when (code) {
                401, 403 -> "TMDB rejected the credential. Check Settings → Integrations → TMDB."
                429 -> "TMDB is busy. Wait before searching again."
                else -> "TMDB is unavailable. Try again later."
            } }
            return connection.inputStream.use { JSONObject(PersonalImages.text(it, 1024 * 1024)) }
        } finally { connection.disconnect() }
    }
}

/** Pure response mapping: people/invalid IDs never become movie links. */
internal fun tmdbCards(response: JSONObject): List<MediaCard> = buildList {
    val results = response.optJSONArray("results") ?: return@buildList
    for (index in 0 until minOf(results.length(), 24)) {
        val item = results.optJSONObject(index) ?: continue
        val kind = item.optString("media_type")
        val id = item.optLong("id", 0)
        if (kind !in listOf("movie", "tv") || id <= 0) continue
        val link = MediaLinks.title(if (kind == "movie") "movie" else "series", "tmdb:$id")
        val path = if (item.isNull("poster_path")) "" else item.optString("poster_path")
        val image = if (path.matches(TMDB_POSTER_PATH_REGEX)) "https://image.tmdb.org/t/p/w342$path" else ""
        val name = item.optString(if (kind == "movie") "title" else "name", "Untitled")
        val date = item.optString(if (kind == "movie") "release_date" else "first_air_date", "")
        val year = date.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
        var metadata = year.orEmpty()
        val rating = item.optDouble("vote_average", Double.NaN)
        if (item.optLong("vote_count", 0) > 0 && rating.isFinite() && rating in 0.0..10.0)
            metadata += (if (metadata.isEmpty()) "" else "\n") + "TMDB " + String.format(java.util.Locale.ROOT, "%.1f", rating)
        val overview = if (item.isNull("overview")) "" else item.optString("overview").trim().take(500)
        add(MediaCard(link, name, image, link, MediaLinks.SOURCE, true, 0, 0, metadata,
            if (kind == "movie") "Movie" else "Series", overview))
    }
}

/** Unknown/null/foreign identifiers cannot silently turn into a different Stremio title. */
internal fun tmdbImdbId(response: JSONObject): String? = response.optString("imdb_id").takeIf { it.matches(IMDB_ID_REGEX) }

/** Normalizes a title for cross-database comparisons, removing articles and punctuation. */
internal fun cleanTitle(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
        .replace(CLEAN_TITLE_YEAR_REGEX, "")
        .replace(CLEAN_TITLE_ARTICLE_REGEX, "")
        .replace(CLEAN_TITLE_NON_ALNUM_REGEX, "")
        .trim().ifEmpty { "raw:" + s.trim() }

/** Tolerant release year matching across cinema databases (handles ±1 year festival/theatrical discrepancies). */
internal fun matchesYear(year1: String, year2: String, tolerance: Int = 1): Boolean {
    val y1 = year1.filter(Char::isDigit).take(4)
    val y2 = year2.filter(Char::isDigit).take(4)
    if (y1.length != 4 || y2.length != 4) return false
    val int1 = y1.toIntOrNull() ?: return false
    val int2 = y2.toIntOrNull() ?: return false
    return kotlin.math.abs(int1 - int2) <= tolerance
}

internal fun mediaKind(label: String): String = if (label.contains("movie", ignoreCase = true)) "movie" else "series"

/** Rank only the requested kind and supplied year; festival releases may differ by one year. */
internal fun matchRecommendation(rec: AiRecommendation, candidates: List<MediaCard>): MediaCard? {
    if (rec.title.isBlank()) return null
    val typed = candidates.filter { mediaKind(it.mediaType) == rec.type && cleanTitle(it.title) == cleanTitle(rec.title) }
    if (rec.year.isBlank()) return typed.firstOrNull()
    return typed.firstOrNull { matchesYear(rec.year, it.metadata.substringBefore("\n").substringBefore(" · "), 0) }
        ?: typed.firstOrNull { matchesYear(rec.year, it.metadata.substringBefore("\n").substringBefore(" · "), 1) }
}
