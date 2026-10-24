package com.asadraza.streamflix.core.data.repository

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.source.FakeCurrentProfileStore
import com.asadraza.streamflix.core.data.source.FakeMyListDataSource
import com.asadraza.streamflix.core.data.source.FakeWatchHistoryDataSource
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoriesTest {
    private val profiles = FakeCurrentProfileStore()
    private val myListSource = FakeMyListDataSource()
    private val historySource = FakeWatchHistoryDataSource()

    private fun TestScope.myList() =
        MyListRepositoryImpl(myListSource, profiles, UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.history() =
        WatchHistoryRepositoryImpl(historySource, profiles, UnconfinedTestDispatcher(testScheduler))

    private fun item(id: String, addedAt: Long = 1) = MyListItem(id, "Title $id", null, addedAt)

    private fun watched(id: String, at: Long = 1, position: Long = 0) =
        WatchedMovie(id, "Title $id", null, null, at, position)

    private suspend fun <T> kotlinx.coroutines.flow.Flow<Result<T>>.last(): Result<T> = toList().last()

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.toList(): List<T> {
        val values = mutableListOf<T>()
        collect { values += it }
        return values
    }

    @Test
    fun `an empty list is reported when no profile is selected`() = runTest {
        myList().observeMyList().test {
            awaitItem() // loading
            assertEquals(Result.Success(emptyList<MyListItem>()), awaitItem())
        }
    }

    @Test
    fun `adding stores the title under the active profile`() = runTest {
        profiles.set("p1")
        val repository = myList()

        assertEquals(Result.Success(Unit), repository.add(item("1")).last())

        assertEquals(listOf(item("1")), myListSource.items.value["p1"])
        assertTrue(repository.observeIsInMyList("1").first())
        assertFalse(repository.observeIsInMyList("2").first())
    }

    @Test
    fun `adding without a profile fails with not signed in`() = runTest {
        val error = myList().add(item("1")).last() as Result.Error

        assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), error.type)
    }

    @Test
    fun `the list follows the active profile and shows newest first`() = runTest {
        profiles.set("p1")
        val repository = myList()
        repository.add(item("old", addedAt = 1)).last()
        repository.add(item("new", addedAt = 2)).last()
        profiles.set("p2")
        repository.add(item("other", addedAt = 3)).last()

        repository.observeMyList().test {
            awaitItem()
            assertEquals(listOf("other"), (awaitItem() as Result.Success).data.map { it.movieId })
            profiles.set("p1")
            assertEquals(listOf("new", "old"), (awaitItem() as Result.Success).data.map { it.movieId })
        }
    }

    @Test
    fun `removing deletes only that title`() = runTest {
        profiles.set("p1")
        val repository = myList()
        repository.add(item("1")).last()
        repository.add(item("2")).last()

        repository.remove("1").last()

        assertEquals(listOf("2"), myListSource.items.value["p1"]?.map { it.movieId })
    }

    @Test
    fun `provider failures are mapped on writes`() = runTest {
        profiles.set("p1")
        myListSource.failure = UnauthenticatedException()

        val error = myList().add(item("1")).last() as Result.Error

        assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), error.type)
    }

    @Test
    fun `recording a watch keeps one entry per title with the latest position`() = runTest {
        profiles.set("p1")
        val repository = history()

        repository.record(watched("1", at = 1, position = 1000))
        assertEquals(Result.Success(Unit), repository.record(watched("1", at = 2, position = 5000)))

        assertEquals(listOf(5000L), historySource.items.value["p1"]?.map { it.positionMillis })
    }

    @Test
    fun `recording without a profile returns an error value instead of throwing`() = runTest {
        val result = history().record(watched("1"))

        assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), (result as Result.Error).type)
    }

    @Test
    fun `recent history is newest first and limited`() = runTest {
        profiles.set("p1")
        val repository = history()
        repeat(25) { repository.record(watched("m$it", at = it.toLong())) }

        repository.observeRecent().test {
            awaitItem()
            val recent = (awaitItem() as Result.Success).data
            assertEquals(20, recent.size)
            assertEquals("m24", recent.first().movieId)
        }
    }

    @Test
    fun `removing from history deletes the entry`() = runTest {
        profiles.set("p1")
        val repository = history()
        repository.record(watched("1"))

        repository.remove("1").last()

        assertEquals(emptyList<WatchedMovie>(), historySource.items.value["p1"])
    }
}
