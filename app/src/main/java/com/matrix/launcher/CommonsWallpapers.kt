package com.matrix.launcher

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class WallpaperCredit(val title: String, val artist: String, val license: String,
    val page: String, val licenseUrl: String) {
    val id: String get() = PersonalImages.hash(page)
}

/** One public metadata query, serial bounded downloads, no credentials or background service. */
internal object CommonsWallpapers {
    private const val AGENT = "MatrixLauncher/1.0 (https://github.com/bitmere/matrix-launcher)"
    /** Compiled once: candidate() runs per page, so building these per call (D-04). */
    private val LICENSE_REGEX = Regex("(?:CC BY(?:-SA)? [0-9.]+|CC0(?: [0-9.]+)?|Public domain)")
    private val STRIP_TAGS_REGEX = Regex("<[^>]*>")
    internal data class Candidate(val url: String, val credit: WallpaperCredit)

    internal fun candidate(page: JSONObject): Candidate? {
        val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        val width = info.optInt("width")
        val height = info.optInt("height")
        if (width < 1920 || height < 1080 || width <= height || width.toDouble() / height > 2.5) return null
        if (info.optString("mime") !in setOf("image/jpeg", "image/png", "image/webp")) return null
        val meta = info.optJSONObject("extmetadata") ?: return null
        fun value(key: String) = meta.optJSONObject(key)?.optString("value").orEmpty()
        val license = value("LicenseShortName")
        val licenseUrl = value("LicenseUrl").let { if (it.startsWith("//")) "https:$it" else it }
        // Skip unknown/restricted licenses; attribution is a required part of the cache entry.
        if (!license.matches(LICENSE_REGEX)) return null
        if (!httpsHost(licenseUrl, "creativecommons.org")) return null
        val artist = value("Artist").replace(STRIP_TAGS_REGEX, "").trim()
        val source = info.optString("descriptionurl")
        val url = info.optString("thumburl", info.optString("url"))
        if (artist.isBlank() || !httpsHost(source, "commons.wikimedia.org") ||
            !(httpsHost(url, "upload.wikimedia.org") || httpsHost(url, "thumb.wikimedia.org"))) return null
        if (info.has("thumbwidth") && (info.optInt("thumbwidth") < 1920 || info.optInt("thumbheight") < 1080)) return null
        val title = page.optString("title").removePrefix("File:")
        if (title.isBlank()) return null
        return Candidate(url, WallpaperCredit(title, artist, license, source, licenseUrl))
    }

    private fun httpsHost(value: String, host: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "https" && uri.host == host && uri.userInfo == null && uri.port == -1
    } catch (_: Exception) { false }

    fun refresh(directory: File, prefs: SharedPreferences, active: () -> Boolean): String {
        var added = 0
        var suitable = 0
        try {
            val offset = prefs.getInt("commons_offset", 0).coerceIn(0, 400)
            val search = URLEncoder.encode("landscape filetype:bitmap incategory:\"Featured pictures on Wikimedia Commons\"", StandardCharsets.UTF_8.name())
            val query = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=20&gsroffset=$offset&gsrsearch=$search&prop=imageinfo&iiprop=url%7Csize%7Cmime%7Cextmetadata&iiurlwidth=1920&maxlag=5"
            val connection = PersonalImages.connect(query, null)
            connection.setRequestProperty("User-Agent", AGENT)
            val response = try {
                if (connection.responseCode != 200) throw IOException("Commons HTTP ${connection.responseCode}")
                connection.inputStream.use { JSONObject(PersonalImages.text(it, 1024 * 1024)) }
            } finally { connection.disconnect() }
            if (response.has("error")) throw IOException("Commons API unavailable")
            val pages = response.optJSONObject("query")?.optJSONObject("pages") ?: return "No matching landscapes. Saved pictures kept."
            val keys = pages.keys()
            while (keys.hasNext() && added < 5 && active()) {
                val item = candidate(pages.getJSONObject(keys.next())) ?: continue
                val name = PersonalImages.hash(item.credit.page)
                val image = File(directory, "$name.jpg")
                if (image.exists()) continue
                suitable++
                val temp = File.createTempFile("commons", ".part", directory)
                val credit = File(directory, "$name.json")
                val creditPart = File(directory, "$name.json.part")
                try {
                    val download = PersonalImages.connect(item.url, null)
                    download.setRequestProperty("User-Agent", AGENT)
                    try {
                        if (download.responseCode != 200) throw IOException("Image unavailable")
                        download.inputStream.use { PersonalImages.copy(it, temp, 8 * 1024 * 1024) }
                    } finally { download.disconnect() }
                    if (!active()) break
                    creditPart.writeText(encode(item.credit).toString())
                    PersonalWallpapers.saveImage(temp, image)
                    if (!creditPart.renameTo(credit)) { image.delete(); throw IOException("Cannot save attribution") }
                    added++
                } catch (error: IOException) {
                    android.util.Log.w("MatrixWallpapers", "Image download failed (${error.javaClass.simpleName})")
                }
                finally { temp.delete(); creditPart.delete() }
            }
            val next = response.optJSONObject("continue")?.optInt("gsroffset", 0) ?: 0
            prefs.edit { putInt("commons_offset", if (next > 400) 0 else next) }
        } catch (error: Exception) {
            android.util.Log.w("MatrixWallpapers", "Commons refresh failed (${error.javaClass.simpleName})")
            return "$added landscapes added. Commons is unavailable; saved pictures kept."
        } finally {
            directory.listFiles { file -> file.extension == "jpg" }?.sortedBy { it.lastModified() }
                ?.dropLast(20)?.forEach { image -> if (image.delete()) File(directory, "${image.nameWithoutExtension}.json").delete() }
            directory.listFiles { file -> file.extension == "json" }?.forEach { credit ->
                if (!File(directory, "${credit.nameWithoutExtension}.jpg").isFile) credit.delete()
            }
        }
        return when {
            added > 0 -> "$added Commons landscapes added. Ready for offline rotation."
            suitable > 0 -> "Could not download the landscape images. Saved pictures kept; retry in one minute."
            else -> "No new suitable HD landscapes in this batch. Saved pictures kept; try the next batch in one minute."
        }
    }

    private fun encode(credit: WallpaperCredit) = JSONObject().put("title", credit.title).put("artist", credit.artist)
        .put("license", credit.license).put("page", credit.page).put("licenseUrl", credit.licenseUrl)

    fun credits(directory: File): List<WallpaperCredit> = directory.listFiles { file -> file.extension == "json" }
        .orEmpty().sortedBy { it.name }.mapNotNull { file ->
            try {
                if (file.length() > 64 * 1024 || !File(directory, "${file.nameWithoutExtension}.jpg").isFile) return@mapNotNull null
                val value = JSONObject(file.readText())
                WallpaperCredit(value.getString("title"), value.getString("artist"), value.getString("license"), value.getString("page"), value.getString("licenseUrl"))
            } catch (_: Exception) { null }
        }
}
