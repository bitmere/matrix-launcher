package com.matrix.launcher

/** Plain values only: the selection and command path can run without Android. */
internal enum class NowPlayingStatus { Playing, Paused, Stopped, Other }
internal enum class NowPlayingCommand { Play, Pause, Previous, Next }
internal data class NowPlayingSession(
    val id: String,
    val title: String,
    val source: String,
    val status: NowPlayingStatus,
    val commands: Set<NowPlayingCommand> = emptySet()
) {
    val toggle: NowPlayingCommand?
        get() = when (status) {
            NowPlayingStatus.Playing -> NowPlayingCommand.Pause
            NowPlayingStatus.Paused -> NowPlayingCommand.Play
            else -> null
        }?.takeIf { it in commands }
}

internal interface NowPlayingSessionStore {
    /** Priority order from the platform; an empty list also represents lost access. */
    fun sessions(): List<NowPlayingSession>
    fun send(id: String, command: NowPlayingCommand)
}

internal object NowPlayingPolicy {
    fun select(sessions: List<NowPlayingSession>): NowPlayingSession? =
        sessions.firstOrNull { it.status == NowPlayingStatus.Playing }
            ?: sessions.firstOrNull { it.status == NowPlayingStatus.Paused }

    fun commands(status: NowPlayingStatus, play: Boolean, pause: Boolean, toggle: Boolean,
                 previous: Boolean, next: Boolean): Set<NowPlayingCommand> = buildSet {
        if (status != NowPlayingStatus.Playing && status != NowPlayingStatus.Paused) return@buildSet
        if (play || toggle) add(NowPlayingCommand.Play)
        if (pause || toggle) add(NowPlayingCommand.Pause)
        if (previous) add(NowPlayingCommand.Previous)
        if (next) add(NowPlayingCommand.Next)
    }

    /** Reject stale UI actions rather than controlling a different app. */
    fun dispatch(store: NowPlayingSessionStore, id: String, command: NowPlayingCommand): Boolean {
        val current = select(store.sessions()) ?: return false
        if (current.id != id || command !in current.commands) return false
        if (command == NowPlayingCommand.Play && current.status != NowPlayingStatus.Paused) return false
        if (command == NowPlayingCommand.Pause && current.status != NowPlayingStatus.Playing) return false
        store.send(id, command)
        return true
    }
}
