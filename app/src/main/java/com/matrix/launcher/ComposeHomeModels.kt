package com.matrix.launcher

import android.content.Intent
import androidx.compose.runtime.Immutable

@Immutable
internal data class LaunchApp(val id: String, val title: String)

@Immutable
internal data class MediaCard(val id: String, val title: String, val image: String, val link: String,
    val source: String = com.matrix.launcher.core.MediaLinks.SOURCE, val portrait: Boolean = true,
    val position: Long = 0L, val duration: Long = 0L, val metadata: String = "",
    val mediaType: String = "", val summary: String = "", val simklReference: String = "")

@Immutable
internal data class MediaRow(val id: String, val title: String, val cards: List<MediaCard>, val nowPlaying: NowPlayingItem? = null)

@Immutable
internal data class HomeSnapshot(val apps: List<LaunchApp> = emptyList(), val favorites: List<String> = emptyList(),
    val rows: List<MediaRow> = emptyList(), val allRows: List<MediaRow> = emptyList(), val status: String = "Loading Home…")

@Immutable
internal data class InputTarget(val id: String, val title: String, val intent: Intent)

