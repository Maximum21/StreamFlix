package com.asadraza.streamflix.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class PaginatedMovieGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun movie(id: String, title: String = "Movie $id") = Movie(
        id = id,
        title = title,
        description = "",
        backdropPath = null,
        posterPath = null,
        releaseDate = "",
        voteAverage = 0.0,
        voteCount = 0,
        genreIds = emptyList(),
        originalLanguage = "en",
        adult = false,
        video = false
    )

    private fun idle() = LoadState.NotLoading(endOfPaginationReached = true)

    private fun setGrid(data: PagingData<Movie>, onClick: (String) -> Unit = {}) {
        composeRule.setContent {
            PaginatedMovieGrid(
                movies = flowOf(data).collectAsLazyPagingItems(),
                onMovieClick = onClick,
                emptyMessage = "Nothing here"
            )
        }
    }

    @Test
    fun `the first load shows placeholder cards instead of replacing the grid with a spinner`() {
        // A bare empty PagingData never reports a load state, which is what the first load looks like
        setGrid(PagingData.empty())
        composeRule.waitForIdle()

        val placeholders = composeRule.onAllNodesWithTag(MOVIE_CARD_PLACEHOLDER_TAG).fetchSemanticsNodes()
        assertTrue("expected placeholder cards, found ${placeholders.size}", placeholders.isNotEmpty())
    }

    @Test
    fun `loaded movies are listed and can be opened`() {
        var opened: String? = null
        setGrid(
            PagingData.from(listOf(movie("1"), movie("2")), sourceLoadStates = LoadStates(idle(), idle(), idle())),
            onClick = { opened = it }
        )

        composeRule.onNodeWithText("Movie 1").assertIsDisplayed()
        composeRule.onNodeWithText("Movie 2").performClick()

        assertEquals("2", opened)
        assertEquals(0, composeRule.onAllNodesWithTag(MOVIE_CARD_PLACEHOLDER_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun `a finished empty result shows the message`() {
        setGrid(PagingData.empty(sourceLoadStates = LoadStates(idle(), idle(), idle())))

        composeRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }
}
