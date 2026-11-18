package com.matrix.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import com.matrix.launcher.core.MediaLinks
import com.matrix.launcher.core.PackageNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-specific contracts stay here; catalog providers only produce typed title identities. */
internal enum class MatrixMediaApp(val label: String, val pkg: String, val probe: String) {
    NUVIO("Nuvio", "com.nuvio.tv", "nuvio://meta/movie/tt0133093"),
    STREMIO("Stremio", "com.stremio.one", "stremio:///detail/movie/tt0133093/tt0133093?autoPlay=false")
}

/** Automatic retains Nuvio on existing installations; explicit missing choices never silently switch. */
internal fun mediaTargetSelection(preferred: String, available: List<MatrixMediaApp>): MatrixMediaApp? =
    if (preferred == "auto") MatrixMediaApp.entries.firstOrNull { it in available }
    else available.firstOrNull { it.name == preferred }

internal class MatrixMediaRouter(private val context: Context? = null, private val prefs: SharedPreferences,
    private val availableOverride: (() -> List<MatrixMediaApp>)? = null,
    private val imdbId: suspend (String, String, String) -> String) {
    var preferred: String
        get() = prefs.getString("media_title_app", "auto").orEmpty()
        set(value) { require(value == "auto" || MatrixMediaApp.entries.any { it.name == value }); prefs.edit { putString("media_title_app", value) } }
    private fun handler(app: MatrixMediaApp) = context?.packageManager?.resolveActivity(
        Intent(Intent.ACTION_VIEW, app.probe.toUri()).setPackage(app.pkg), 0)?.activityInfo
        ?.takeIf { it.packageName == app.pkg && it.exported && it.enabled }
    fun available() = availableOverride?.invoke() ?: MatrixMediaApp.entries.filter { handler(it) != null }

    suspend fun intent(card: MediaCard): Intent = withContext(Dispatchers.IO) {
        // Matrix catalog identities are separate from a publisher's playback request.
        if (card.source == MediaLinks.SOURCE && card.link.startsWith("matrix-title://")) {
            val title = requireNotNull(MediaLinks.parseTitle(card.link)) { "This catalog title is unsupported." }
            val app = requireNotNull(mediaTargetSelection(preferred, available())) {
                "To stream movies and series, install Stremio or Nuvio on your TV."
            }
            return@withContext when (app) {
                MatrixMediaApp.NUVIO -> nuvioDetail(title.type, title.id)
                MatrixMediaApp.STREMIO -> {
                    val id = if (title.id.startsWith("tt")) title.id else imdbId(title.type, title.id, card.simklReference)
                    val link = MediaLinks.stremio(title.type, id)
                    require(link.isNotBlank()) { "Stremio needs an IMDb identity for this title. This title has no supported mapping; choose another media app." }
                    Intent(Intent.ACTION_VIEW, link.toUri()).setPackage(app.pkg).addFlags(mediaDetailFlags(app))
                }
            }
        }
        if (card.source == MatrixMediaApp.NUVIO.pkg && card.link.startsWith("nuvio://")) {
            val title = requireNotNull(MediaLinks.parseTitle(card.link)) { "This Nuvio title link is unsupported." }
            return@withContext nuvioDetail(title.type, title.id)
        }
        // Native app channels/Watch Next always return to their original publisher, not the preference.
        val intent = Intent.parseUri(card.link, Intent.URI_INTENT_SCHEME)
        require(PackageNames.validPackage(card.source)) { "This source app cannot open the title." }
        require(intent.component == null || intent.component!!.packageName == card.source) { "Link points outside its source app." }
        intent.selector = null; intent.clipData = null; intent.`package` = card.source
        if (card.source == MatrixMediaApp.NUVIO.pkg && intent.getStringExtra("launchMode") != "stream") {
            MediaLinks.parseTitle(intent.dataString)?.let { return@withContext nuvioDetail(it.type, it.id) }
            val type = intent.getStringExtra("contentType"); val id = intent.getStringExtra("contentId")
            if (!type.isNullOrBlank() && !id.isNullOrBlank()) return@withContext nuvioDetail(type, id)
        }
        intent.flags = mediaLaunchFlags(intent.flags, card.source == MatrixMediaApp.NUVIO.pkg)
        intent
    }
    private fun nuvioDetail(type: String, id: String): Intent {
        val app = MatrixMediaApp.NUVIO
        val target = requireNotNull(handler(app)) { "Install a compatible Nuvio app to open this title." }
        // Beta 10 creates fresh details without videoId/name; no URI means no second navigation.
        return Intent(Intent.ACTION_VIEW).setComponent(ComponentName(target.packageName, target.name)).setPackage(app.pkg)
            .putExtra("contentId", id).putExtra("contentType", type).putExtra("launchMode", "stream")
            .addFlags(mediaDetailFlags(app))
    }
}

internal fun mediaLaunchFlags(published: Int, forceDelivery: Boolean = false): Int {
    val routing = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    return Intent.FLAG_ACTIVITY_NEW_TASK or (if (forceDelivery) routing else published and routing)
}

/** Stremio's standard Activity reads links on creation; SINGLE_TOP bypasses that path in 1.12.1. */
internal fun mediaDetailFlags(app: MatrixMediaApp): Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
    (if (app == MatrixMediaApp.NUVIO) Intent.FLAG_ACTIVITY_SINGLE_TOP else 0)
