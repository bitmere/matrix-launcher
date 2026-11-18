package com.matrix.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

internal data class WebSnippet(
    val title: String,
    val snippet: String,
    val source: String = ""
)

/**
 * Lightweight, in-app web search and factual context retrieval engine for Matrix Launcher.
 * Executes free live searches (DuckDuckGo HTML + Wikipedia API) directly from Android TV
 * without requiring any paid search API keys, proxies, or external servers.
 */
internal object MatrixWebSearchEngine {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Smart_TV_Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour

    /** Bounded LRU (D-05): the old unbounded map grew for the life of the process. */
    private val cache = BoundedCache<String, Pair<Long, List<WebSnippet>>>(32)

    private val snippetRegex = Pattern.compile(
        """<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    private val titleRegex = Pattern.compile(
        """<a[^>]*class="result__a"[^>]*>(.*?)</a>""",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    /** Compiled once: cleanHtml runs per snippet/title, so building these per call (D-04). */
    private val htmlTagRegex = Regex("<[^>]+>")
    private val htmlDecEntityRegex = Regex("&#(\\d+);")
    private val htmlHexEntityRegex = Regex("&#x([0-9a-fA-F]+);")
    private val htmlWhitespaceRegex = Regex("\\s+")

    /**
     * Performs an in-app live web search for the query, combining DuckDuckGo and Wikipedia results.
     * Guaranteed to never throw; returns empty list on timeout or network errors.
     */
    suspend fun search(query: String, maxResults: Int = 8, forceRefresh: Boolean = false): List<WebSnippet> = withContext(Dispatchers.IO) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return@withContext emptyList()

        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = synchronized(cache) { cache[webSearchCacheKey(normalized, maxResults)] }
            if (cached != null && (now - cached.first) < CACHE_TTL_MS) {
                return@withContext cached.second
            }
        }

        // Both sources in parallel (D-05): DuckDuckGo HTML + Wikipedia API.
        // Wikipedia is fetched even when DuckDuckGo alone would suffice — both
        // are free keyless endpoints, and the parallel fetch is ~2x faster
        // worst case for the AI flows that block on these lookups.
        val results = coroutineScope {
            val ddgDeferred = async { runCatching { fetchDuckDuckGo(query) }.getOrDefault(emptyList()) }
            val wikiDeferred = async { runCatching { fetchWikipedia(query) }.getOrDefault(emptyList()) }
            val combined = mutableListOf<WebSnippet>()
            combined.addAll(ddgDeferred.await())
            for (wiki in wikiDeferred.await()) {
                if (combined.none { it.title.equals(wiki.title, ignoreCase = true) }) {
                    combined.add(wiki)
                }
            }
            combined
        }

        val limited = results.take(maxResults.coerceIn(0, 8))
        if (limited.isNotEmpty()) {
            synchronized(cache) { cache[webSearchCacheKey(normalized, maxResults)] = Pair(now, limited) }
        }
        limited
    }

    private fun fetchDuckDuckGo(query: String): List<WebSnippet> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoint = "https://html.duckduckgo.com/html/?q=$encoded"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }

        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            // Bounded read (D-05): the old uncapped readText() could buffer an
            // unbounded page; over 512 KB the fetch fails into an empty result.
            val html = conn.inputStream.use { PersonalImages.text(it, 512 * 1024) }
            parseDuckDuckGoHtml(html)
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseDuckDuckGoHtml(html: String): List<WebSnippet> {
        val list = mutableListOf<WebSnippet>()
        val snippetMatcher = snippetRegex.matcher(html)
        val titleMatcher = titleRegex.matcher(html)

        val snippets = mutableListOf<String>()
        while (snippetMatcher.find()) {
            val clean = cleanHtml(snippetMatcher.group(1).orEmpty())
            if (clean.isNotBlank()) snippets.add(clean)
        }

        val titles = mutableListOf<String>()
        while (titleMatcher.find()) {
            val clean = cleanHtml(titleMatcher.group(1).orEmpty())
            if (clean.isNotBlank()) titles.add(clean)
        }

        val count = minOf(snippets.size, titles.size)
        for (i in 0 until count) {
            val t = titles[i]
            val s = snippets[i]
            if (s.length >= 15) {
                list.add(WebSnippet(title = t, snippet = s, source = "DuckDuckGo"))
            }
        }
        return list
    }

    private fun fetchWikipedia(query: String): List<WebSnippet> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoint = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 3000
            readTimeout = 3500
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            // Bounded read (D-05), same 512 KB cap as DuckDuckGo above.
            val text = conn.inputStream.use { PersonalImages.text(it, 512 * 1024) }
            parseWikipediaJson(text)
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseWikipediaJson(jsonText: String): List<WebSnippet> {
        val list = mutableListOf<WebSnippet>()
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return emptyList()
        val queryObj = root.optJSONObject("query") ?: return emptyList()
        val searchArr = queryObj.optJSONArray("search") ?: return emptyList()

        for (i in 0 until searchArr.length()) {
            val item = searchArr.optJSONObject(i) ?: continue
            val title = cleanHtml(item.optString("title"))
            val snippet = cleanHtml(item.optString("snippet"))
            if (title.isNotBlank() && snippet.isNotBlank()) {
                list.add(WebSnippet(title = title, snippet = snippet, source = "Wikipedia"))
            }
        }
        return list
    }

    /** Cleans HTML tags, whitespace, and decodes common HTML entities including numeric codes. */
    internal fun cleanHtml(raw: String): String {
        return raw
            .replace(htmlTagRegex, " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&hellip;", "…")
            .replace("&#8216;", "‘")
            .replace("&#8217;", "’")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace(htmlDecEntityRegex) { match ->
                runCatching {
                    val code = match.groupValues[1].toInt()
                    if (code in 32..65535) code.toChar().toString() else ""
                }.getOrDefault("")
            }
            .replace(htmlHexEntityRegex) { match ->
                runCatching {
                    val code = match.groupValues[1].toInt(16)
                    if (code in 32..65535) code.toChar().toString() else ""
                }.getOrDefault("")
            }
            .replace(htmlWhitespaceRegex, " ")
            .trim()
    }

    /** Formats web search snippets into a compact, token-efficient prompt block for LLMs. */
    fun formatPromptContext(snippets: List<WebSnippet>): String {
        if (snippets.isEmpty()) return ""
        val unique = LinkedHashMap<String, WebSnippet>()
        for (s in snippets) {
            val key = s.title.lowercase().trim()
            if (key.isNotBlank() && !unique.containsKey(key)) {
                unique[key] = s
            }
        }
        val sb = StringBuilder()
        sb.append("LIVE REAL-TIME WEB CONTEXT (from current web search):\n")
        var count = 0
        for ((_, s) in unique) {
            count++
            val cleanSnippet = s.snippet.take(180).trim()
            sb.append("$count. [${s.title}]: $cleanSnippet\n")
            if (count >= 8) break
        }
        sb.append("Use the live web context above to prioritize accurate real-world release years, official titles, and verified availability.\n\n")
        return sb.toString()
    }

    fun clearCache() {
        synchronized(cache) { cache.clear() }
    }
}

internal fun webSearchCacheKey(query: String, count: Int) = "${query.trim().lowercase(java.util.Locale.ROOT)}:${count.coerceIn(0, 8)}"
