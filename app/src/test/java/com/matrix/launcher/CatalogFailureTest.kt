package com.matrix.launcher

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogFailureTest {
    private val query = "matrix"
    private val movieError = IllegalStateException("Movie catalog unavailable")
    private val seriesError = IllegalStateException("Series catalog unavailable")
    private val movies = listOf(
        MediaCard("m1", "Matrix Reloaded", "", "", "", true, 0, 0),
        MediaCard("m2", "The Matrix", "", "", "", true, 0, 0)
    )
    private val series = listOf(
        MediaCard("s1", "Matrix Revisited", "", "", "", true, 0, 0),
        MediaCard("s2", "Matrix Explained", "", "", "", true, 0, 0)
    )

    @Test fun combineBothFailuresKeepsMovieFailure() {
        val result = combineCatalogResults(Result.failure(movieError), Result.failure(seriesError), query)
        assertSame(movieError, result.exceptionOrNull())
    }

    @Test fun combineMovieFailureKeepsRankedSeries() {
        val result = combineCatalogResults(Result.failure(movieError), Result.success(series), query)
        assertEquals(CinemetaClient.rankResults(series, emptyList(), query), result.getOrThrow())
    }

    @Test fun combineSeriesFailureKeepsRankedMovies() {
        val result = combineCatalogResults(Result.success(movies), Result.failure(seriesError), query)
        assertEquals(CinemetaClient.rankResults(movies, emptyList(), query), result.getOrThrow())
    }

    @Test fun combineTwoEmptySuccessesReturnsEmptySuccess() {
        val result = combineCatalogResults(Result.success(emptyList()), Result.success(emptyList()), query)
        assertEquals(emptyList<MediaCard>(), result.getOrThrow())
    }

    @Test fun combineBothSuccessesRanksAndInterleavesCards() {
        val result = combineCatalogResults(Result.success(movies), Result.success(series), query)
        assertEquals(CinemetaClient.rankResults(movies, series, query), result.getOrThrow())
    }

    @Test fun blankCatalogQueryReturnsEmptySuccess() {
        assertEquals(emptyList<MediaCard>(), CinemetaClient.searchCatalog("movie", "  ").getOrThrow())
    }

    @Test fun searchBothFailuresThrowsCatalogUnavailable() {
        val repository = repository(Result.failure(movieError), Result.failure(seriesError))
        assertThrows(CatalogUnavailableException::class.java) {
            runBlocking { repository.search(MatrixSearchProvider.CATALOG, query) }
        }
    }

    @Test fun searchOneFailureReturnsSuccessfulSide() = runBlocking {
        val repository = repository(Result.success(movies), Result.failure(seriesError))
        assertEquals(
            CinemetaClient.rankResults(movies, emptyList(), query),
            repository.search(MatrixSearchProvider.CATALOG, query)
        )
    }

    @Test fun searchBothSuccessesReturnsRankedCards() = runBlocking {
        val repository = repository(Result.success(movies), Result.success(series))
        assertEquals(
            CinemetaClient.rankResults(movies, series, query),
            repository.search(MatrixSearchProvider.CATALOG, query)
        )
    }

    @Test fun searchBothEmptySuccessesReturnsEmptyList() = runBlocking {
        val repository = repository(Result.success(emptyList()), Result.success(emptyList()))
        assertEquals(emptyList<MediaCard>(), repository.search(MatrixSearchProvider.CATALOG, query))
    }

    private fun repository(
        movieResult: Result<List<MediaCard>>,
        seriesResult: Result<List<MediaCard>>
    ) = MatrixSearchRepository(mapBackedPrefs(), catalogSearch = { type, actualQuery ->
        assertEquals(query, actualQuery)
        when (type) {
            "movie" -> movieResult
            "series" -> seriesResult
            else -> error("Unexpected catalog type: $type")
        }
    })
}
