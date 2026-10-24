package com.asadraza.streamflix.feature.mylist

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val results = MutableSharedFlow<Result<List<MyListItem>>>(extraBufferCapacity = 8)
    private val observeMyList = mockk<ObserveMyListUseCase>().also { every { it() } returns results }

    private fun item(id: String) = MyListItem(id, "Title $id", null, 1)

    @Test
    fun `starts loading and shows the list once it arrives`() {
        val viewModel = MyListViewModel(observeMyList)
        assertTrue(viewModel.state.value.isLoading)

        results.tryEmit(Result.Success(listOf(item("1"), item("2"))))

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf("1", "2"), viewModel.state.value.items.map { it.movieId })
    }

    @Test
    fun `a failed refresh keeps the last list and reports the error`() {
        val viewModel = MyListViewModel(observeMyList)
        results.tryEmit(Result.Success(listOf(item("1"))))

        results.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))

        assertEquals(listOf("1"), viewModel.state.value.items.map { it.movieId })
        assertEquals(ErrorType.Http(500), viewModel.state.value.errorType)
    }

    @Test
    fun `a failed refresh with titles on screen sends a message, repeatedly`() = runTest {
        val viewModel = MyListViewModel(observeMyList)
        results.tryEmit(Result.Success(listOf(item("1"))))

        viewModel.effect.test {
            results.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))
            assertTrue(awaitItem() is MyListEffect.ShowMessage)

            results.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))
            assertTrue(awaitItem() is MyListEffect.ShowMessage)
        }
    }

    @Test
    fun `a failed first load has nothing to keep and sends no message`() = runTest {
        val viewModel = MyListViewModel(observeMyList)

        viewModel.effect.test {
            results.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))
            expectNoEvents()
        }
        assertEquals(ErrorType.Http(500), viewModel.state.value.errorType)
    }

    @Test
    fun `opening an item navigates to its detail`() = runTest {
        val viewModel = MyListViewModel(observeMyList)

        viewModel.effect.test {
            viewModel.onEvent(MyListEvent.OnItemClick("42"))
            assertEquals(MyListEffect.NavigateToDetail("42"), awaitItem())
        }
    }

    @Test
    fun `retry resubscribes to the list`() {
        val viewModel = MyListViewModel(observeMyList)
        results.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))

        viewModel.onEvent(MyListEvent.OnRetry)
        results.tryEmit(Result.Success(listOf(item("1"))))

        assertEquals(null, viewModel.state.value.errorType)
        assertEquals(1, viewModel.state.value.items.size)
    }
}
