package com.matrix.launcher

import android.net.Uri
import com.matrix.launcher.core.MediaLinks
import org.json.JSONObject

/**
 * Unified, modular client for the Cinemeta media catalog (Stremio's free, keyless metadata provider).
 * Consolidates catalog searches, trending metadata fetches, detail lookups, and stream availability checks.
 */
internal object CinemetaClient {

    private const val BASE_URL = "https://v3-cinemeta.strem.io"

    /**
     * Searches Cinemeta catalog for a given media type ("movie" or "series") and query.
     */
    fun searchCatalog(type: String, query: String): Result<List<MediaCard>> = runCatching {
        val clean = query.trim()
        if (clean.isBlank()) return@runCatching emptyList()
        val encoded = Uri.encode(clean)
        val url = "$BASE_URL/catalog/$type/top/search=$encoded.json"
        val conn = PersonalImages.connect(url, null)
        try {
            val code = conn.responseCode
            check(code == 200) { "Cinemeta catalog returned HTTP $code" }
            val json = conn.inputStream.use { JSONObject(PersonalImages.text(it, 512 * 1024)) }
            parseCards(json, type)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Fetches top trending metas for "movie" or "series", keeping at most
     * [limit] items. Callers only use the first two; the default keeps the
     * old behaviour for any other caller.
     */
    fun getTopMetas(type: String, limit: Int = Int.MAX_VALUE): List<JSONObject> = runCatching {
        val url = "$BASE_URL/catalog/$type/top.json"
        val conn = PersonalImages.connect(url, null)
        try {
            if (conn.responseCode == 200) {
                val json = conn.inputStream.use { JSONObject(PersonalImages.text(it, 256 * 1024)) }
                parseTopMetas(json, limit)
            } else emptyList()
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(emptyList())

    /** Parses the top-metas array, keeping at most [limit] items. */
    internal fun parseTopMetas(json: JSONObject, limit: Int): List<JSONObject> {
        val arr = json.optJSONArray("metas") ?: return emptyList()
        return (0 until minOf(arr.length(), limit)).mapNotNull { arr.optJSONObject(it) }
    }

    /**
     * Fetches metadata for a specific media item by IMDb ID (e.g. "tt0133093").
     */
    fun getMeta(type: String, imdbId: String): JSONObject? = runCatching {
        if (!imdbId.startsWith("tt")) return@runCatching null
        val url = "$BASE_URL/meta/$type/$imdbId.json"
        val conn = PersonalImages.connect(url, null)
        try {
            if (conn.responseCode == 200) {
                val json = conn.inputStream.use { JSONObject(PersonalImages.text(it, 512 * 1024)) }
                json.optJSONObject("meta")
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * Checks if a movie or series title is indexed on Cinemeta for direct playback,
     * searching movies first and falling back to series if unindexed.
     */
    fun checkAvailability(title: String, year: String): String {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return "Missing title"
        val encoded = Uri.encode(cleanTitle)

        fun queryType(type: String): Pair<JSONObject?, Int> {
            val url = "$BASE_URL/catalog/$type/top/search=$encoded.json"
            val conn = PersonalImages.connect(url, null)
            return try {
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.use { JSONObject(PersonalImages.text(it, 256 * 1024)) }
                    val metas = json.optJSONArray("metas")
                    if (metas != null && metas.length() > 0) {
                        var bestMeta: JSONObject? = null
                        if (year.isNotBlank()) {
                            for (i in 0 until metas.length()) {
                                val item = metas.optJSONObject(i) ?: continue
                                val itemYear = item.optString("releaseInfo", "")
                                if (matchesYear(year, itemYear)) {
                                    bestMeta = item
                                    break
                                }
                            }
                        }
                        (bestMeta ?: metas.getJSONObject(0)) to 200
                    } else null to 200
                } else null to conn.responseCode
            } catch (_: Exception) {
                null to -1
            } finally {
                conn.disconnect()
            }
        }

        // Try movie first
        val (movieMeta, movieCode) = queryType("movie")
        if (movieMeta != null) {
            val id = movieMeta.optString("id", "")
            val rating = movieMeta.optString("imdbRating", "")
            val rel = movieMeta.optString("releaseInfo", year)
            val name = movieMeta.optString("name", cleanTitle)
            return "Found on Cinemeta: \"$name\" ($rel), IMDb: $id, Rating: ${rating.ifBlank { "Unrated" }}. Playable on Nuvio and Stremio."
        }

        // Try series
        val (seriesMeta, seriesCode) = queryType("series")
        if (seriesMeta != null) {
            val id = seriesMeta.optString("id", "")
            val rating = seriesMeta.optString("imdbRating", "")
            val rel = seriesMeta.optString("releaseInfo", year)
            val name = seriesMeta.optString("name", cleanTitle)
            return "Found on Cinemeta (Series): \"$name\" ($rel), IMDb: $id, Rating: ${rating.ifBlank { "Unrated" }}. Playable on Nuvio and Stremio."
        }

        if (movieCode == -1 && seriesCode == -1) {
            return "Catalog check unavailable."
        }
        return "Not yet indexed on Cinemeta for direct Stremio playback. Can still be recommended with official title and year."
    }

    /**
     * Parses a Cinemeta API JSON response into a typed list of [MediaCard]s.
     */
    fun parseCards(response: JSONObject, fallbackType: String = "movie"): List<MediaCard> = buildList {
        val metas = response.optJSONArray("metas") ?: return@buildList
        for (index in 0 until minOf(metas.length(), 24)) {
            val item = metas.optJSONObject(index) ?: continue
            val id = item.optString("id", "").trim()
            if (!id.startsWith("tt")) continue
            val rawType = item.optString("type", fallbackType)
            val type = if (rawType == "series" || rawType == "tv") "series" else "movie"
            val name = item.optString("name", "Untitled").trim()
            val poster = item.optString("poster", "").trim()
            val releaseInfo = item.optString("releaseInfo", "").trim()
            val imdbRating = item.optString("imdbRating", "").trim()
            val ratingSuffix = if (imdbRating.isNotBlank()) " · IMDb $imdbRating" else ""
            val metadata = (releaseInfo.ifBlank { if (type == "movie") "Movie" else "Series" } + ratingSuffix).trim()
            val description = item.optString("description", "").trim().take(500)
            val link = MediaLinks.title(type, id)
            add(
                MediaCard(
                    id = "cinemeta:$id",
                    title = name,
                    image = poster,
                    link = link,
                    source = MediaLinks.SOURCE,
                    portrait = true,
                    position = 0L,
                    duration = 0L,
                    metadata = metadata,
                    mediaType = if (type == "movie") "Movie" else "Series",
                    summary = description
                )
            )
        }
    }

    /**
     * Interleaves movie and series results and ranks them by exact/prefix query match tier.
     */
    fun rankResults(movies: List<MediaCard>, series: List<MediaCard>, query: String): List<MediaCard> {
        val clean = query.trim()
        val cleanWithoutThe = clean.removePrefix("The ").removePrefix("the ").trim()

        fun tier(card: MediaCard): Int {
            val title = card.title.trim()
            val titleWithoutThe = title.removePrefix("The ").removePrefix("the ").trim()
            return when {
                title.equals(clean, ignoreCase = true) -> 0
                titleWithoutThe.equals(cleanWithoutThe, ignoreCase = true) -> 1
                title.startsWith(clean, ignoreCase = true) || titleWithoutThe.startsWith(cleanWithoutThe, ignoreCase = true) -> 2
                else -> 3
            }
        }

        val interleaved = buildList(movies.size + series.size) {
            val m = movies.iterator()
            val s = series.iterator()
            while (m.hasNext() || s.hasNext()) {
                if (m.hasNext()) add(m.next())
                if (s.hasNext()) add(s.next())
            }
        }

        return interleaved.sortedBy { tier(it) }
    }
}
