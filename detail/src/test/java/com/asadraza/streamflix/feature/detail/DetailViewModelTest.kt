package com.asadraza.streamflix.feature.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.library.AddToMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveIsInMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromMyListUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import com.asadraza.streamflix.core.testing.testMovieDetail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val getMovieDetails = mockk<GetMovieDetailsUseCase>()
    private val inMyList = MutableStateFlow(false)
    private val observeIsInMyList = mockk<ObserveIsInMyListUseCase>().also { every { it("42") } returns inMyList }
    private val addToMyList = mockk<AddToMyListUseCase>()
    private val removeFromMyList = mockk<RemoveFromMyListUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val detail = testMovieDetail("42")

    private fun createViewModel() = DetailViewModel(
        getMovieDetails,
        observeIsInMyList,
        addToMyList,
        removeFromMyList,
        analytics,
        SavedStateHandle(mapOf("movieId" to "42"))
    )

    @Test
    fun `loads the movie named in the navigation arguments`() {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Loading(), Result.Success(detail))

        assertEquals(DetailState.Success(detail), createViewModel().state.value)
    }

    @Test
    fun `shows an error when nothing could be loaded`() {
        val type = ErrorType.Http(500)
        coEvery { getMovieDetails("42") } returns flowOf(Result.Error(Exception(), type))

        assertEquals(DetailState.Error(type), createViewModel().state.value)
    }

    @Test
    fun `keeps showing content when a refresh fails`() {
        coEvery { getMovieDetails("42") } returns
            flowOf(Result.Success(detail), Result.Error(Exception(), ErrorType.Http(500)))

        assertEquals(DetailState.Success(detail), createViewModel().state.value)
    }

    @Test
    fun `retry reloads after an error`() {
        coEvery { getMovieDetails("42") } returnsMany listOf(
            flowOf(Result.Error(Exception(), ErrorType.Http(500))),
            flowOf(Result.Success(detail))
        )
        val viewModel = createViewModel()

        viewModel.onEvent(DetailEvent.OnRetry)

        assertEquals(DetailState.Success(detail), viewModel.state.value)
    }

    @Test
    fun `play trailer navigates to the player with the movie id`() = runTest {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(DetailEvent.OnPlayTrailer)
            assertEquals(DetailEffect.NavigateToPlayer("42"), awaitItem())
        }
    }

    @Test
    fun `back navigates back`() = runTest {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(DetailEvent.OnBackClick)
            assertTrue(awaitItem() is DetailEffect.NavigateBack)
        }
    }

    @Test
    fun `toggling adds the movie to My List with a snapshot of its title`() {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        val added = slot<MyListItem>()
        every { addToMyList(capture(added)) } returns flowOf(Result.Success(Unit))
        val viewModel = createViewModel()

        viewModel.onEvent(DetailEvent.OnToggleMyList)

        assertEquals("42", added.captured.movieId)
        assertEquals(detail.title, added.captured.title)
        verify { analytics.log(AnalyticsEvent.MyListChanged("42", added = true)) }
    }

    @Test
    fun `toggling removes a movie that is already in My List`() {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        every { removeFromMyList("42") } returns flowOf(Result.Success(Unit))
        inMyList.value = true
        val viewModel = createViewModel()

        viewModel.onEvent(DetailEvent.OnToggleMyList)

        verify { removeFromMyList("42") }
        verify { analytics.log(AnalyticsEvent.MyListChanged("42", added = false)) }
    }

    @Test
    fun `a failed My List change is reported`() = runTest {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        every { addToMyList(any()) } returns flowOf(Result.Error(Exception(), ErrorType.Http(500)))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(DetailEvent.OnToggleMyList)
            assertTrue(awaitItem() is DetailEffect.ShowMessage)
        }
    }

    @Test
    fun `toggling before the movie has loaded does nothing`() {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Loading())
        val viewModel = createViewModel()

        viewModel.onEvent(DetailEvent.OnToggleMyList)

        verify(exactly = 0) { addToMyList(any()) }
    }

    @Test
    fun `opening a similar title navigates to its detail`() = runTest {
        coEvery { getMovieDetails("42") } returns flowOf(Result.Success(detail))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(DetailEvent.OnSimilarClick("7"))
            assertEquals(DetailEffect.NavigateToDetail("7"), awaitItem())
        }
    }
}
