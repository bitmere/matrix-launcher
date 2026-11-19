package com.matrix.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixPosterStateTest {
    @Test fun missingUrlUsesTheNoImageStateForEveryOutcome() {
        for (outcome in listOf(null, PosterOutcome.Loading, PosterOutcome.Loaded, PosterOutcome.Failed)) {
            assertEquals(PosterState.NoImage, posterStateFor(false, outcome))
        }
    }

    @Test fun urlUsesTheRequestOutcomeAndStartsLoading() {
        assertEquals(PosterState.Loading, posterStateFor(true, null))
        assertEquals(PosterState.Loading, posterStateFor(true, PosterOutcome.Loading))
        assertEquals(PosterState.Loaded, posterStateFor(true, PosterOutcome.Loaded))
        assertEquals(PosterState.Error, posterStateFor(true, PosterOutcome.Failed))
    }
}
