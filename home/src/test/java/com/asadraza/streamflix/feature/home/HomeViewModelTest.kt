package com.asadraza.streamflix.feature.home

import androidx.paging.PagingData
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.flags.ObserveFeatureFlagsUseCase
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryPaginatedUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromWatchHistoryUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.model.settings.FeatureFlags
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val getMoviesByCategoryPaginated = mockk<GetMoviesByCategoryPaginatedUseCase>().also {
        every { it(any()) } returns flowOf(PagingData.empty())
    }
    private val history = MutableSharedFlow<Result<List<WatchedMovie>>>(extraBufferCapacity = 4)
    private val myList = MutableSharedFlow<Result<List<MyListItem>>>(extraBufferCapacity = 4)
    private val observeWatchHistory = mockk<ObserveWatchHistoryUseCase>().also { every { it() } returns history }
    private val observeMyList = mockk<ObserveMyListUseCase>().also { every { it() } returns myList }
    private val flags = MutableStateFlow(FeatureFlags())
    private val observeFeatureFlags = mockk<ObserveFeatureFlagsUseCase>().also { every { it() } returns flags }
    private val removeFromWatchHistory = mockk<RemoveFromWatchHistoryUseCase>()

    private fun createViewModel() = HomeViewModel(
        getMoviesByCategoryPaginated,
        observeWatchHistory,
        observeMyList,
        observeFeatureFlags,
        removeFromWatchHistory
    )

    private fun watched(id: String) = WatchedMovie(id, "Title $id", null, null, 1, 1_000)

    @Test
    fun `there is one feed per category in display order`() {
        val viewModel = createViewModel()

        assertEquals(MovieCategory.getAll(), viewModel.categoryFeeds.keys.toList())
        MovieCategory.getAll().forEach { verify { getMoviesByCategoryPaginated(it) } }
    }

    @Test
    fun `library rows follow the profile`() {
        val viewModel = createViewModel()

        history.tryEmit(Result.Success(listOf(watched("1"))))
        myList.tryEmit(Result.Success(listOf(MyListItem("2", "Two", null, 1))))

        assertEquals(listOf("1"), viewModel.state.value.continueWatching.map { it.movieId })
        assertEquals(listOf("2"), viewModel.state.value.myList.map { it.movieId })
    }

    @Test
    fun `the hero banner follows the remote flag`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value.showHeroBanner)

        flags.value = FeatureFlags(showHeroBanner = false)

        assertFalse(viewModel.state.value.showHeroBanner)
    }

    @Test
    fun `a failing library row keeps what was shown`() {
        val viewModel = createViewModel()
        history.tryEmit(Result.Success(listOf(watched("1"))))

        history.tryEmit(Result.Error(Exception(), ErrorType.Http(500)))

        assertEquals(listOf("1"), viewModel.state.value.continueWatching.map { it.movieId })
    }

    @Test
    fun `clicking a movie navigates to its detail`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.OnMovieClick("42"))
            assertEquals(HomeEffect.NavigateToDetail("42"), awaitItem())
        }
    }

    @Test
    fun `play opens the player`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.OnPlayClick("42"))
            assertEquals(HomeEffect.NavigateToPlayer("42"), awaitItem())
        }
    }

    @Test
    fun `search click navigates to search`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.OnSearchClick)
            assertTrue(awaitItem() is HomeEffect.NavigateToSearch)
        }
    }

    @Test
    fun `removing a continue watching entry deletes it from history`() {
        every { removeFromWatchHistory("1") } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()

        viewModel.onEvent(HomeEvent.OnRemoveContinueWatching("1"))

        verify { removeFromWatchHistory("1") }
    }
}
