package com.matrix.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.HandlerThread
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal data class NowPlayingItem(val session: NowPlayingSession, val artwork: Any?)

internal interface NowPlayingCommandTarget {
    fun command(id: String, command: NowPlayingCommand)
}

/** In-memory only; the listener owns the adapter and clears it when access ends. */
internal object NowPlayingMedia {
    private val mutable = MutableStateFlow<NowPlayingItem?>(null)
    val state = mutable.asStateFlow()
    @Volatile private var adapter: NowPlayingCommandTarget? = null
    @Synchronized fun attach(owner: NowPlayingCommandTarget) { adapter = owner; mutable.value = null }
    @Synchronized fun detach(owner: NowPlayingCommandTarget) {
        if (adapter === owner) { adapter = null; mutable.value = null }
    }
    @Synchronized fun publish(owner: NowPlayingCommandTarget, value: NowPlayingItem?) {
        if (adapter === owner) mutable.value = value
    }
    fun command(id: String, command: NowPlayingCommand) { adapter?.command(id, command) }
}

/** All framework reads, package lookups and transport calls are serialized off the UI thread. */
internal class AndroidNowPlaying(context: Context) : NowPlayingSessionStore, NowPlayingCommandTarget {
    private val app = context.applicationContext
    private val thread = HandlerThread("MatrixNowPlaying").apply { start() }
    private val handler = Handler(thread.looper)
    private val manager by lazy { app.getSystemService(MediaSessionManager::class.java) }
    private val listenerComponent = ComponentName(app.packageName, "com.gathin.tvhome.GuardRevival")
    private data class Entry(val id: String, val controller: MediaController, val callback: MediaController.Callback,
        var metadataLoaded: Boolean = false, var artwork: Any? = null)
    private val entries = linkedMapOf<MediaSession.Token, Entry>()
    private var registered = false
    @Volatile private var closed = false
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        guarded { replace(controllers.orEmpty()) }
    }

    fun start() {
        NowPlayingMedia.attach(this)
        handler.post {
            guarded {
                manager?.addOnActiveSessionsChangedListener(listener, listenerComponent, handler)
                registered = manager != null
                replace(manager?.getActiveSessions(listenerComponent).orEmpty())
            }
        }
    }

    private fun guarded(block: () -> Unit) {
        if (closed) return
        try { block() } catch (_: RuntimeException) {
            // Access may be revoked or a remote session may die between any two Binder calls.
            clearEntries()
            NowPlayingMedia.publish(this, null)
        }
    }

    private fun replace(controllers: List<MediaController>) {
        val tokens = controllers.map { it.sessionToken }.toSet()
        entries.keys.filter { it !in tokens }.forEach { token ->
            entries.remove(token)?.let { runCatching { it.controller.unregisterCallback(it.callback) } }
        }
        val ordered = linkedMapOf<MediaSession.Token, Entry>()
        controllers.forEach { controller ->
            val token = controller.sessionToken
            ordered[token] = entries[token] ?: run {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) { guarded { publish() } }
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        guarded {
                            entries[token]?.let {
                                it.metadataLoaded = true
                                it.artwork = runCatching { artwork(metadata) }.getOrNull()
                            }
                            publish()
                        }
                    }
                    override fun onSessionDestroyed() {
                        guarded {
                            entries.remove(token)?.let { runCatching { it.controller.unregisterCallback(it.callback) } }
                            publish()
                        }
                    }
                }
                val entry = Entry(UUID.randomUUID().toString(), controller, callback)
                // Track before registration so partial failures also unregister new callbacks.
                entries[token] = entry
                controller.registerCallback(callback, handler)
                entry
            }
        }
        entries.clear()
        entries.putAll(ordered)
        publish()
    }

    override fun sessions(): List<NowPlayingSession> = entries.values.map { entry ->
        val controller = entry.controller
        val state = controller.playbackState
        val status = when (state?.state) {
            PlaybackState.STATE_PLAYING -> NowPlayingStatus.Playing
            PlaybackState.STATE_PAUSED -> NowPlayingStatus.Paused
            PlaybackState.STATE_STOPPED -> NowPlayingStatus.Stopped
            else -> NowPlayingStatus.Other
        }
        val actions = state?.actions ?: 0L
        fun supports(action: Long) = actions and action != 0L
        val metadata = controller.metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: app.getString(R.string.now_playing_title)
        NowPlayingSession(entry.id, title, TvFeed.appName(app, controller.packageName), status,
            NowPlayingPolicy.commands(status, supports(PlaybackState.ACTION_PLAY), supports(PlaybackState.ACTION_PAUSE),
                supports(PlaybackState.ACTION_PLAY_PAUSE), supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS), supports(PlaybackState.ACTION_SKIP_TO_NEXT)))
    }

    private fun publish() {
        val selected = NowPlayingPolicy.select(sessions())
        val entry = entries.values.firstOrNull { it.id == selected?.id }
        if (entry != null && !entry.metadataLoaded) {
            entry.metadataLoaded = true
            entry.artwork = runCatching { artwork(entry.controller.metadata) }.getOrNull()
        }
        NowPlayingMedia.publish(this, selected?.let { NowPlayingItem(it, entry?.artwork) })
    }

    private fun artwork(metadata: MediaMetadata?): Any? = metadata?.let { value ->
        val bitmap = listOf(MediaMetadata.METADATA_KEY_DISPLAY_ICON, MediaMetadata.METADATA_KEY_ART, MediaMetadata.METADATA_KEY_ALBUM_ART)
            .firstNotNullOfOrNull { value.getBitmap(it) }
        if (bitmap != null) thumbnail(bitmap) else {
            listOf(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                .firstNotNullOfOrNull { value.getString(it)?.takeIf { uri ->
                    uri.startsWith("content://") || uri.startsWith("https://") || uri.startsWith("android.resource://")
                } }
        }
    }

    private fun thumbnail(bitmap: Bitmap): Bitmap {
        val edge = maxOf(bitmap.width, bitmap.height)
        if (edge <= 256) return bitmap
        return bitmap.scale((bitmap.width * 256 / edge).coerceAtLeast(1),
            (bitmap.height * 256 / edge).coerceAtLeast(1))
    }

    override fun command(id: String, command: NowPlayingCommand) {
        handler.post { guarded { NowPlayingPolicy.dispatch(this, id, command) } }
    }

    override fun send(id: String, command: NowPlayingCommand) {
        val controls = entries.values.firstOrNull { it.id == id }?.controller?.transportControls ?: return
        when (command) {
            NowPlayingCommand.Play -> controls.play()
            NowPlayingCommand.Pause -> controls.pause()
            NowPlayingCommand.Previous -> controls.skipToPrevious()
            NowPlayingCommand.Next -> controls.skipToNext()
        }
    }

    private fun clearEntries() {
        entries.values.forEach { runCatching { it.controller.unregisterCallback(it.callback) } }
        entries.clear()
    }

    fun close() {
        closed = true
        NowPlayingMedia.detach(this)
        handler.post {
            if (registered) runCatching { manager?.removeOnActiveSessionsChangedListener(listener) }
            clearEntries()
            thread.quitSafely()
        }
    }
}
