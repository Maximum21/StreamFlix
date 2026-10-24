package com.asadraza.streamflix.feature.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.library.AddToMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveIsInMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromMyListUseCase
import com.asadraza.streamflix.core.model.movie.CastMember
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.model.movie.RelatedMovie
import com.asadraza.streamflix.core.testing.testMovieDetail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class DetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var playedMovie: String? = null
    private var openedMovie: String? = null
    private val inMyList = MutableStateFlow(false)

    private fun setScreen(result: Result<MovieDetail>) {
        val useCase = mockk<GetMovieDetailsUseCase>()
        coEvery { useCase("1") } returns flowOf(result)
        val observeIsInMyList = mockk<ObserveIsInMyListUseCase>()
        every { observeIsInMyList("1") } returns inMyList
        val viewModel = DetailViewModel(
            useCase,
            observeIsInMyList,
            mockk<AddToMyListUseCase>(),
            mockk<RemoveFromMyListUseCase>(),
            mockk(relaxed = true),
            SavedStateHandle(mapOf("movieId" to "1"))
        )
        composeRule.setContent {
            DetailScreen(
                onBackClick = {},
                onPlayClick = { playedMovie = it },
                onMovieClick = { openedMovie = it },
                viewModel = viewModel
            )
        }
    }

    @Test
    fun `a movie with a trailer offers playback`() {
        setScreen(Result.Success(testMovieDetail("1")))

        composeRule.onNodeWithText("Play Trailer").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals("1", playedMovie)
    }

    @Test
    fun `a movie without a trailer hides the play button`() {
        setScreen(Result.Success(testMovieDetail("1", videos = emptyList())))

        composeRule.onNodeWithText("Overview").assertIsDisplayed()
        composeRule.onNodeWithText("Play Trailer").assertDoesNotExist()
    }

    @Test
    fun `a failed load shows the error with a retry action`() {
        setScreen(Result.Error(Exception(), ErrorType.Http(500)))

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `the My List button reflects membership`() {
        setScreen(Result.Success(testMovieDetail("1")))
        composeRule.onNodeWithText("My List").assertIsDisplayed()

        inMyList.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("In My List").assertIsDisplayed()
    }

    @Test
    fun `cast, director and similar titles are listed and similar titles open`() {
        val detail = testMovieDetail("1").copy(
            director = "Lana Wachowski",
            cast = listOf(CastMember(1, "Keanu Reeves", "Neo", null)),
            similar = listOf(RelatedMovie("9", "Constantine", null))
        )
        setScreen(Result.Success(detail))

        composeRule.onNodeWithText("Directed by Lana Wachowski").assertIsDisplayed()
        composeRule.onNodeWithText("Keanu Reeves").assertIsDisplayed()
        composeRule.onNodeWithText("Neo").assertIsDisplayed()
        composeRule.onNodeWithText("More like this").assertIsDisplayed()
        composeRule.onNodeWithText("Constantine").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("9", openedMovie)
    }
}
