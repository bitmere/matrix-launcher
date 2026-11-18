package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixAccountNavigationTest {
    @Test fun savingListIdReplacesFormAndBackReturnsToSimkl() {
        val history = mutableListOf("settings", "simkl", "simkl-add")
        var destination = ""
        openSimklListId("123", { history.removeAt(history.lastIndex) }) { destination = it; history.add("list") }
        assertEquals("simkl-list:123", destination)
        assertEquals(listOf("settings", "simkl", "list"), history)
        history.removeAt(history.lastIndex)
        assertEquals("simkl", history.last())
    }

    @Test fun approvalFromClientFormReturnsToAccountNotTheForm() {
        assertEquals(listOf("settings", "simkl"), connectedAccountHistory(listOf("settings", "simkl", "simkl-client", "simkl-connect")))
    }
    @Test fun approvalWithExistingIdAndDirectEntryKeepTheCorrectParent() {
        assertEquals(listOf("settings", "simkl"), connectedAccountHistory(listOf("settings", "simkl", "simkl-connect")))
        assertEquals(listOf("simkl"), connectedAccountHistory(listOf("simkl-connect")))
        assertEquals(listOf("settings", "simkl"), connectedAccountHistory(listOf("settings", "simkl")))
    }
}
