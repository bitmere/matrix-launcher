package com.matrix.launcher

import org.junit.Assert.*
import org.junit.Test

class NowPlayingSessionTest {
    private fun session(id: String = "one", status: NowPlayingStatus = NowPlayingStatus.Playing,
                        commands: Set<NowPlayingCommand> = NowPlayingCommand.entries.toSet()) =
        NowPlayingSession(id, "Title", "App", status, commands)

    private class Store(var values: List<NowPlayingSession>) : NowPlayingSessionStore {
        val sent = mutableListOf<Pair<String, NowPlayingCommand>>()
        override fun sessions() = values
        override fun send(id: String, command: NowPlayingCommand) { sent += id to command }
    }

    @Test fun playingAndPausedAreVisibleButStoppedAndOtherAreNot() {
        for (status in NowPlayingStatus.entries) {
            val item = session(status = status)
            assertEquals(if (status == NowPlayingStatus.Playing || status == NowPlayingStatus.Paused) item else null,
                NowPlayingPolicy.select(listOf(item)))
        }
        assertNull(NowPlayingPolicy.select(emptyList()))
    }

    @Test fun playingWinsOverPausedAndPriorityBreaksTies() {
        val paused = session("paused", NowPlayingStatus.Paused)
        val first = session("first")
        val second = session("second")
        assertEquals(first, NowPlayingPolicy.select(listOf(paused, first, second)))
        assertEquals(paused, NowPlayingPolicy.select(listOf(paused, session("other", NowPlayingStatus.Paused))))
    }

    @Test fun toggleFollowsStateAndAdvertisedActions() {
        assertEquals(NowPlayingCommand.Pause, session().toggle)
        assertEquals(NowPlayingCommand.Play, session(status = NowPlayingStatus.Paused).toggle)
        assertNull(session(commands = setOf(NowPlayingCommand.Play)).toggle)
        assertNull(session(status = NowPlayingStatus.Paused, commands = setOf(NowPlayingCommand.Pause)).toggle)
        assertNull(session(status = NowPlayingStatus.Stopped).toggle)
    }

    @Test fun actionsMapIndependentlyAndDoNotInventSkip() {
        assertEquals(emptySet<NowPlayingCommand>(), NowPlayingPolicy.commands(NowPlayingStatus.Playing, false, false, false, false, false))
        assertEquals(setOf(NowPlayingCommand.Play, NowPlayingCommand.Pause),
            NowPlayingPolicy.commands(NowPlayingStatus.Paused, false, false, true, false, false))
        assertEquals(setOf(NowPlayingCommand.Pause, NowPlayingCommand.Next),
            NowPlayingPolicy.commands(NowPlayingStatus.Playing, false, true, false, false, true))
        assertEquals(setOf(NowPlayingCommand.Play, NowPlayingCommand.Previous),
            NowPlayingPolicy.commands(NowPlayingStatus.Paused, true, false, false, true, false))
        for (status in listOf(NowPlayingStatus.Stopped, NowPlayingStatus.Other)) {
            assertTrue(NowPlayingPolicy.commands(status, true, true, true, true, true).isEmpty())
        }
    }

    @Test fun commandsGoOnlyToSelectedLiveSession() {
        val store = Store(listOf(session()))
        assertTrue(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Pause))
        assertTrue(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Next))
        store.values = listOf(session(status = NowPlayingStatus.Paused))
        assertTrue(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Play))
        assertEquals(listOf("one" to NowPlayingCommand.Pause, "one" to NowPlayingCommand.Next, "one" to NowPlayingCommand.Play), store.sent)
    }

    @Test fun staleIdentityRemovedActionChangedStateAndEndedSessionRejectCommands() {
        val store = Store(listOf(session("replacement")))
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Pause))
        store.values = listOf(session(commands = emptySet()))
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Next))
        store.values = listOf(session(status = NowPlayingStatus.Paused))
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Pause))
        store.values = listOf(session())
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Play))
        store.values = listOf(session(status = NowPlayingStatus.Stopped))
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Next))
        store.values = emptyList()
        assertFalse(NowPlayingPolicy.dispatch(store, "one", NowPlayingCommand.Play))
        assertTrue(store.sent.isEmpty())
    }

    @Test fun oldPausedSessionIsNotControlledWhenAnotherAppStartsPlaying() {
        val store = Store(listOf(session("old", NowPlayingStatus.Paused), session("new")))
        assertFalse(NowPlayingPolicy.dispatch(store, "old", NowPlayingCommand.Play))
        assertTrue(store.sent.isEmpty())
    }

    @Test fun mediaHubRoutesCommandsOnlyToCurrentOwnerAndClearsOnDetach() {
        val first = object : NowPlayingCommandTarget {
            val commands = mutableListOf<Pair<String, NowPlayingCommand>>()
            override fun command(id: String, command: NowPlayingCommand) { commands += id to command }
        }
        val second = object : NowPlayingCommandTarget {
            val commands = mutableListOf<Pair<String, NowPlayingCommand>>()
            override fun command(id: String, command: NowPlayingCommand) { commands += id to command }
        }
        try {
            NowPlayingMedia.attach(first)
            val item = NowPlayingItem(session("one"), null)
            NowPlayingMedia.publish(first, item)
            assertEquals(item, NowPlayingMedia.state.value)
            NowPlayingMedia.command("one", NowPlayingCommand.Pause)
            assertEquals(listOf("one" to NowPlayingCommand.Pause), first.commands)

            NowPlayingMedia.attach(second)
            assertNull(NowPlayingMedia.state.value)
            NowPlayingMedia.publish(first, item)
            assertNull(NowPlayingMedia.state.value)

            NowPlayingMedia.detach(first)
            NowPlayingMedia.command("one", NowPlayingCommand.Next)
            assertEquals(listOf("one" to NowPlayingCommand.Next), second.commands)

            NowPlayingMedia.publish(second, item)
            NowPlayingMedia.detach(second)
            assertNull(NowPlayingMedia.state.value)
        } finally {
            NowPlayingMedia.detach(first)
            NowPlayingMedia.detach(second)
        }
    }
}
