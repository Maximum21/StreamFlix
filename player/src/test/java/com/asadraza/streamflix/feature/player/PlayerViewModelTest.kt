package com.asadraza.streamflix.feature.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RecordWatchUseCase
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import com.asadraza.streamflix.core.testing.testMovieDetail
import com.asadraza.streamflix.core.testing.testVideo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val getMovieDetails = mockk<GetMovieDetailsUseCase>()
    private val observeWatchHistory = mockk<ObserveWatchHistoryUseCase>().also {
        every { it() } returns flowOf(Result.Loading(), Result.Success(emptyList<WatchedMovie>()))
    }
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val recordWatch = mockk<RecordWatchUseCase>().also { coEvery { it(any()) } returns Result.Success(Unit) }

    private fun createViewModel() = PlayerViewModel(
        SavedStateHandle(mapOf("movieId" to "7")),
        getMovieDetails,
        observeWatchHistory,
        recordWatch,
        analytics
    )

    @Test
    fun `plays the first YouTube trailer, ignoring other videos`() {
        val videos = listOf(
            testVideo(key = "teaser", type = "Teaser"),
            testVideo(key = "vimeo", site = "Vimeo"),
            testVideo(key = "trailer")
        )
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7", videos)))

        val state = createViewModel().state.value

        assertEquals("trailer", state.trailerKey)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a title with a single non-trailer video has no trailer`() {
        coEvery { getMovieDetails("7") } returns
            flowOf(Result.Success(testMovieDetail("7", listOf(testVideo(type = "Clip")))))

        val state = createViewModel().state.value

        assertNull(state.trailerKey)
        assertTrue(state.hasNoTrailer)
    }

    @Test
    fun `a title without videos has no trailer`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7", emptyList())))

        assertTrue(createViewModel().state.value.hasNoTrailer)
    }

    @Test
    fun `errors are exposed and are not reported as a missing trailer`() {
        val type = ErrorType.Http(500)
        coEvery { getMovieDetails("7") } returns flowOf(Result.Error(Exception(), type))

        val state = createViewModel().state.value

        assertEquals(type, state.errorType)
        assertFalse(state.hasNoTrailer)
    }

    @Test
    fun `back navigates back`() = runTest {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(PlayerEvent.OnBackPressed)
            assertEquals(PlayerEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `resumes from the position saved in the history`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        every { observeWatchHistory() } returns flowOf(
            Result.Loading(),
            Result.Success(
                listOf(
                    WatchedMovie("7", "T", null, null, 1, 42_000),
                    WatchedMovie("8", "U", null, null, 1, 9)
                )
            )
        )

        assertEquals(42_000L, createViewModel().state.value.resumeAtMillis)
    }

    @Test
    fun `starts from the beginning when the history cannot be read`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        every { observeWatchHistory() } returns flowOf(Result.Error(Exception(), ErrorType.Http(500)))

        assertEquals(0L, createViewModel().state.value.resumeAtMillis)
    }

    @Test
    fun `position updates are written at most every ten seconds`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        val viewModel = createViewModel()

        listOf(0L, 500L, 5_000L, 9_999L, 10_000L, 12_000L, 20_000L).forEach {
            viewModel.onEvent(PlayerEvent.OnPositionChanged(it))
        }

        // 0 (first tick), 10_000 and 20_000
        coVerify(exactly = 3) { recordWatch(any()) }
    }

    @Test
    fun `leaving the screen records the final position`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        val viewModel = createViewModel()
        viewModel.onEvent(PlayerEvent.OnPositionChanged(0L))

        viewModel.onEvent(PlayerEvent.OnPlaybackStopped(3_000L))

        coVerify { recordWatch(match { it.movieId == "7" && it.positionMillis == 3_000L }) }
    }

    @Test
    fun `nothing is recorded before the movie is known`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Error(Exception(), ErrorType.Http(500)))
        val viewModel = createViewModel()

        viewModel.onEvent(PlayerEvent.OnPositionChanged(0L))

        coVerify(exactly = 0) { recordWatch(any()) }
    }

    @Test
    fun `a trailer play is counted once, when playback really starts`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        val viewModel = createViewModel()

        viewModel.onEvent(PlayerEvent.OnPositionChanged(0L))
        viewModel.onEvent(PlayerEvent.OnPositionChanged(500L))

        verify(exactly = 1) { analytics.log(AnalyticsEvent.TrailerPlayed("7")) }
    }

    @Test
    fun `leaving before playback starts is not a play`() {
        coEvery { getMovieDetails("7") } returns flowOf(Result.Success(testMovieDetail("7")))
        val viewModel = createViewModel()

        viewModel.onEvent(PlayerEvent.OnPlaybackStopped(0L))

        verify(exactly = 0) { analytics.log(any()) }
    }
}
