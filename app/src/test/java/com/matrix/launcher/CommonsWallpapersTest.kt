package com.matrix.launcher

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CommonsWallpapersTest {
    private fun page(): JSONObject {
        val metadata = JSONObject()
        mapOf("Artist" to "<a>Photographer</a>", "LicenseShortName" to "CC BY-SA 4.0",
            "LicenseUrl" to "https://creativecommons.org/licenses/by-sa/4.0/").forEach { (key, value) ->
            metadata.put(key, JSONObject().put("value", value))
        }
        return JSONObject().put("title", "File:Landscape.jpg").put("imageinfo", JSONArray().put(JSONObject()
            .put("width", 3840).put("height", 2160).put("mime", "image/jpeg")
            .put("thumburl", "https://thumb.wikimedia.org/wikipedia/commons/thumb/landscape.jpg/1920px-landscape.jpg?utm_source=commons.wikimedia.org")
            .put("thumbwidth", 1920).put("thumbheight", 1080)
            .put("descriptionurl", "https://commons.wikimedia.org/wiki/File:Landscape.jpg").put("extmetadata", metadata)))
    }
    @Test fun acceptsHdLandscapeWithCompleteAttribution() {
        val item = CommonsWallpapers.candidate(page())!!
        assertEquals("Photographer", item.credit.artist)
        assertEquals("Landscape.jpg", item.credit.title)
        assertEquals("CC BY-SA 4.0", item.credit.license)
        val originalHost = page()
        originalHost.getJSONArray("imageinfo").getJSONObject(0).put("thumburl", "https://upload.wikimedia.org/wikipedia/commons/landscape.jpg")
        assertNotNull(CommonsWallpapers.candidate(originalHost))
    }
    @Test fun rejectsPortraitSmallAndPanoramaImages() {
        for ((width, height) in listOf(1080 to 1920, 1280 to 720, 8000 to 1200)) {
            val page = page(); page.getJSONArray("imageinfo").getJSONObject(0).put("width", width).put("height", height)
            assertNull(CommonsWallpapers.candidate(page))
        }
    }
    @Test fun rejectsMissingCreditsUnclearLicensesAndForeignDownloadHosts() {
        for (field in listOf("Artist", "LicenseShortName", "LicenseUrl")) {
            val page = page(); page.getJSONArray("imageinfo").getJSONObject(0).getJSONObject("extmetadata").remove(field)
            assertNull(CommonsWallpapers.candidate(page))
        }
        for (url in listOf("http://upload.wikimedia.org/a.jpg", "https://upload.wikimedia.org.evil.test/a.jpg", "https://upload.wikimedia.org@evil.test/a.jpg", "https://thumb.wikimedia.org.evil.test/a.jpg")) {
            val page = page(); page.getJSONArray("imageinfo").getJSONObject(0).put("thumburl", url)
            assertNull(CommonsWallpapers.candidate(page))
        }
    }
}
