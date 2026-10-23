package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class GenreMovieRemoteMediatorTest {
    @get:Rule
    val database = InMemoryDatabaseRule()

    private val remote = FakeRemoteDataSource()

    private fun mediator(vararg genres: Int) = GenreMovieRemoteMediator(genres.toSet(), remote, database.local)

    @Test
    fun `refresh stores the discovered movies under the genre selection`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 3, ids = 1..3))

        val result = mediator(28, 12).load(LoadType.REFRESH, emptyPagingState())

        assertEquals(true, result is RemoteMediator.MediatorResult.Success)
        assertEquals(listOf(setOf(28, 12)), remote.requestedGenres)
        val stored = database.local.getMoviesByCategoryPagingSource(GenreMovieRemoteMediator.listId(setOf(12, 28)))
        assertEquals(listOf("1", "2", "3"), stored.ids())
    }

    @Test
    fun `the same genres in a different order share one cached list`() {
        assertEquals(GenreMovieRemoteMediator.listId(setOf(28, 12)), GenreMovieRemoteMediator.listId(setOf(12, 28)))
    }

    @Test
    fun `append continues from the stored next page`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 3, ids = 1..2), 2 to page(2, totalPages = 3, ids = 3..4))
        mediator(28).load(LoadType.REFRESH, emptyPagingState())

        mediator(28).load(LoadType.APPEND, emptyPagingState())

        assertEquals(listOf(1, 2), remote.requestedPages)
        val stored = database.local.getMoviesByCategoryPagingSource(GenreMovieRemoteMediator.listId(setOf(28)))
        assertEquals(listOf("1", "2", "3", "4"), stored.ids())
    }

    @Test
    fun `a recent selection is not fetched again`() = runTest {
        database.local.saveCategoryPage(
            GenreMovieRemoteMediator.listId(setOf(28)),
            emptyList(),
            0,
            RemoteKeyEntity(
                RemoteKeyIds.list(GenreMovieRemoteMediator.listId(setOf(28))),
                null,
                2,
                1,
                createdAt = System.currentTimeMillis()
            ),
            clearExisting = false
        )

        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator(28).initialize())
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator(35).initialize())
    }
}
