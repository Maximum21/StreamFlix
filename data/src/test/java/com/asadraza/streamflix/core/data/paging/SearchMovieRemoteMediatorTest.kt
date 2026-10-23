package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchMovieRemoteMediatorTest {
    @get:Rule
    val database = InMemoryDatabaseRule()

    private val remote = FakeRemoteDataSource()

    private fun mediator(query: String) = SearchMovieRemoteMediator(query, remote, database.local)

    private suspend fun SearchMovieRemoteMediator.run(type: LoadType) = load(type, emptyPagingState())

    @Test
    fun `different queries keep separate results`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..3))
        mediator("batman").run(LoadType.REFRESH)
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 50..51))
        mediator("matrix").run(LoadType.REFRESH)

        assertEquals(listOf("1", "2", "3"), database.local.searchMoviesPagingSource("batman").ids())
        assertEquals(listOf("50", "51"), database.local.searchMoviesPagingSource("matrix").ids())
        assertEquals(listOf("batman", "matrix"), remote.requestedQueries)
    }

    @Test
    fun `refreshing a query replaces only that query`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..3))
        mediator("batman").run(LoadType.REFRESH)
        mediator("matrix").run(LoadType.REFRESH)
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 7..7))

        mediator("batman").run(LoadType.REFRESH)

        assertEquals(listOf("7"), database.local.searchMoviesPagingSource("batman").ids())
        assertEquals(listOf("1", "2", "3"), database.local.searchMoviesPagingSource("matrix").ids())
    }

    @Test
    fun `search results never appear in a category list`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..3))
        mediator("batman").run(LoadType.REFRESH)

        assertTrue(database.local.getMoviesByCategoryPagingSource(MovieCategory.Trending.id).ids().isEmpty())
    }

    @Test
    fun `append continues the same query`() = runTest {
        remote.pages = mapOf(
            1 to page(1, totalPages = 2, ids = 1..20),
            2 to page(2, totalPages = 2, ids = 21..25)
        )
        val mediator = mediator("batman")
        mediator.run(LoadType.REFRESH)

        mediator.run(LoadType.APPEND)

        assertEquals((1..25).map(Int::toString), database.local.searchMoviesPagingSource("batman").ids())
    }

    @Test
    fun `errors are reported without touching stored results`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..3))
        mediator("batman").run(LoadType.REFRESH)
        remote.failure = NetworkError.Timeout

        val result = mediator("batman").run(LoadType.REFRESH)

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(3, database.local.searchMoviesPagingSource("batman").ids().size)
    }
}
