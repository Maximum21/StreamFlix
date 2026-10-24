package com.asadraza.streamflix.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class CategoryMovieRemoteMediatorTest {
    @get:Rule
    val database = InMemoryDatabaseRule()

    private val remote = FakeRemoteDataSource()

    private fun mediator(category: MovieCategory = MovieCategory.Trending) =
        CategoryMovieRemoteMediator(category, remote, database.local)

    private suspend fun CategoryMovieRemoteMediator.run(type: LoadType) = load(type, emptyPagingState())

    @Test
    fun `refresh stores the first page and a key pointing at the next one`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 3, ids = 1..20))

        val result = mediator().run(LoadType.REFRESH)

        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals((1..20).map(Int::toString), database.local.getMoviesByCategoryPagingSource("trending").ids())
        val key = database.local.getRemoteKey(RemoteKeyIds.category("trending"))
        assertEquals(2, key?.nextKey)
        assertNull(key?.prevKey)
    }

    @Test
    fun `append loads the next page after the existing rows`() = runTest {
        remote.pages = mapOf(
            1 to page(1, totalPages = 2, ids = 1..20),
            2 to page(2, totalPages = 2, ids = 21..40)
        )
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)

        val result = mediator.run(LoadType.APPEND)

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals((1..40).map(Int::toString), database.local.getMoviesByCategoryPagingSource("trending").ids())
        assertNull(database.local.getRemoteKey(RemoteKeyIds.category("trending"))?.nextKey)
    }

    @Test
    fun `append without a key starts the list again`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 3, ids = 1..2))
        val result = mediator().run(LoadType.APPEND)

        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(1), remote.requestedPages)
        assertEquals(listOf("1", "2"), database.local.getMoviesByCategoryPagingSource("trending").ids())
    }

    @Test
    fun `appending a page does not make a stale list look fresh`() = runTest {
        remote.pages = mapOf(2 to page(2, totalPages = 3, ids = 3..4))
        database.local.saveCategoryPage(
            "trending",
            emptyList(),
            0,
            RemoteKeyEntity(RemoteKeyIds.category("trending"), null, 2, 1, createdAt = 1_000L),
            clearExisting = false
        )

        mediator().run(LoadType.APPEND)

        assertEquals(1_000L, database.local.getRemoteKey(RemoteKeyIds.category("trending"))?.createdAt)
    }

    @Test
    fun `prepend is never needed`() = runTest {
        val result = mediator().run(LoadType.PREPEND)

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `failed refresh keeps the cached list`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 2, ids = 1..20))
        mediator().run(LoadType.REFRESH)
        remote.failure = NetworkError.NoInternet

        val result = mediator().run(LoadType.REFRESH)

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(20, database.local.getMoviesByCategoryPagingSource("trending").ids().size)
        assertEquals(2, database.local.getRemoteKey(RemoteKeyIds.category("trending"))?.nextKey)
    }

    @Test
    fun `refresh replaces the previous list`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..20))
        mediator().run(LoadType.REFRESH)
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 100..104))

        mediator().run(LoadType.REFRESH)

        assertEquals(
            listOf("100", "101", "102", "103", "104"),
            database.local.getMoviesByCategoryPagingSource("trending").ids()
        )
    }

    @Test
    fun `a movie shared by two categories survives refreshing one of them`() = runTest {
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 1..3))
        mediator(MovieCategory.Trending).run(LoadType.REFRESH)
        mediator(MovieCategory.Popular).run(LoadType.REFRESH)
        remote.pages = mapOf(1 to page(1, totalPages = 1, ids = 10..11))

        mediator(MovieCategory.Trending).run(LoadType.REFRESH)

        assertEquals(listOf("1", "2", "3"), database.local.getMoviesByCategoryPagingSource("popular").ids())
        assertEquals(listOf("10", "11"), database.local.getMoviesByCategoryPagingSource("trending").ids())
    }

    @Test
    fun `an empty response ends pagination`() = runTest {
        remote.pages = mapOf(1 to emptyPage(1, totalPages = 5))

        val result = mediator().run(LoadType.REFRESH)

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `cancellation is not turned into a mediator error`() = runTest {
        remote.failure = CancellationException("cancelled")

        try {
            mediator().run(LoadType.REFRESH)
            org.junit.Assert.fail("Expected cancellation to propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun `initialize skips the refresh while the cache is fresh`() = runTest {
        database.local.saveCategoryPage(
            "trending",
            emptyList(),
            0,
            RemoteKeyEntity(RemoteKeyIds.category("trending"), null, 2, 1, createdAt = System.currentTimeMillis()),
            clearExisting = false
        )

        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator().initialize())
    }

    @Test
    fun `initialize refreshes a missing or expired cache`() = runTest {
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator().initialize())

        database.local.saveCategoryPage(
            "trending",
            emptyList(),
            0,
            RemoteKeyEntity(RemoteKeyIds.category("trending"), null, 2, 1, createdAt = 0L),
            clearExisting = false
        )

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator().initialize())
    }
}
