package com.asadraza.streamflix.core.database

import androidx.paging.PagingSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyIds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalDataSourceTest {
    @get:Rule
    val database = DatabaseTestRule()

    private val local get() = database.localDataSource

    private suspend fun PagingSource<Int, MovieEntity>.ids(): List<String> {
        val result = load(PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false))
        return (result as PagingSource.LoadResult.Page).data.map { it.id }
    }

    private fun key(id: String, next: Int? = 2) =
        RemoteKeyEntity(id = id, prevKey = null, nextKey = next, currentPage = 1)

    private suspend fun saveCategory(category: String, ids: List<String>, start: Int = 0, clear: Boolean = false) =
        local.saveCategoryPage(
            category,
            ids.map { movieEntity(it) },
            start,
            key(RemoteKeyIds.category(category)),
            clear
        )

    private suspend fun saveSearch(query: String, ids: List<String>, start: Int = 0, clear: Boolean = false) =
        local.saveSearchPage(
            query,
            ids.map { movieEntity(it) },
            start,
            key(RemoteKeyIds.search(query)),
            clear
        )

    @Test
    fun `same movie appears in two categories`() = runTest {
        saveCategory("trending", listOf("1", "2"))
        saveCategory("popular", listOf("2", "3"))

        assertEquals(listOf("1", "2"), local.getMoviesByCategoryPagingSource("trending").ids())
        assertEquals(listOf("2", "3"), local.getMoviesByCategoryPagingSource("popular").ids())
    }

    @Test
    fun `refreshing one category keeps shared movies in the other`() = runTest {
        saveCategory("trending", listOf("1", "2"))
        saveCategory("popular", listOf("2", "3"))

        saveCategory("trending", listOf("4"), clear = true)

        assertEquals(listOf("4"), local.getMoviesByCategoryPagingSource("trending").ids())
        assertEquals(listOf("2", "3"), local.getMoviesByCategoryPagingSource("popular").ids())
    }

    @Test
    fun `positions order the list across appended pages`() = runTest {
        saveCategory("trending", listOf("a", "b"), start = 0)
        saveCategory("trending", listOf("c", "d"), start = 2)

        assertEquals(listOf("a", "b", "c", "d"), local.getMoviesByCategoryPagingSource("trending").ids())
    }

    @Test
    fun `updating a movie does not drop its list memberships`() = runTest {
        saveCategory("trending", listOf("1", "2"))
        saveCategory("popular", listOf("1"))

        // Same movie arrives again with new data from another list
        local.saveCategoryPage(
            "popular",
            listOf(movieEntity("1").copy(title = "Renamed")),
            0,
            key(RemoteKeyIds.category("popular")),
            clearExisting = false
        )

        assertEquals(listOf("1", "2"), local.getMoviesByCategoryPagingSource("trending").ids())
    }

    @Test
    fun `different search queries do not share results`() = runTest {
        saveSearch("batman", listOf("1", "2"))
        saveSearch("matrix", listOf("3"))

        assertEquals(listOf("1", "2"), local.searchMoviesPagingSource("batman").ids())
        assertEquals(listOf("3"), local.searchMoviesPagingSource("matrix").ids())
    }

    @Test
    fun `refreshing a search replaces only that query`() = runTest {
        saveSearch("batman", listOf("1", "2"))
        saveSearch("matrix", listOf("3"))

        saveSearch("batman", listOf("9"), clear = true)

        assertEquals(listOf("9"), local.searchMoviesPagingSource("batman").ids())
        assertEquals(listOf("3"), local.searchMoviesPagingSource("matrix").ids())
    }

    @Test
    fun `search results do not leak into categories`() = runTest {
        saveCategory("trending", listOf("1"))
        saveSearch("batman", listOf("2"))

        assertEquals(listOf("1"), local.getMoviesByCategoryPagingSource("trending").ids())
    }

    @Test
    fun `movies no longer in any list are deleted`() = runTest {
        saveCategory("trending", listOf("1", "2"))

        saveCategory("trending", listOf("3"), clear = true)

        assertEquals(1, database.db.movieDao().countMovies())
    }

    @Test
    fun `clearing a category resets its remote key`() = runTest {
        saveCategory("trending", listOf("1"))
        val id = RemoteKeyIds.category("trending")
        assertNotNull(local.getRemoteKey(id))

        local.saveCategoryPage(
            "trending",
            listOf(movieEntity("2")),
            0,
            RemoteKeyEntity(id, null, null, 1),
            clearExisting = true
        )

        val key = local.getRemoteKey(id)
        assertNotNull(key)
        assertNull(key?.nextKey)
    }

    @Test
    fun `clearing the cache removes lists, search results and remote keys`() = runTest {
        saveCategory("trending", listOf("1", "2"))
        saveSearch("batman", listOf("3"))

        local.clearCache()

        assertEquals(emptyList<String>(), local.getMoviesByCategoryPagingSource("trending").ids())
        assertEquals(emptyList<String>(), local.searchMoviesPagingSource("batman").ids())
        assertNull(local.getRemoteKey(RemoteKeyIds.category("trending")))
        assertNull(local.getRemoteKey(RemoteKeyIds.search("batman")))
    }
}
