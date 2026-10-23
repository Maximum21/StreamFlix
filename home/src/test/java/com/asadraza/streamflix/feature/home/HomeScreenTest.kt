package com.asadraza.streamflix.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.flags.ObserveFeatureFlagsUseCase
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryPaginatedUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromWatchHistoryUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.settings.FeatureFlags
import com.asadraza.streamflix.core.testing.testMovie
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h2400dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var played: String? = null
    private var opened: String? = null

    private fun setScreen(
        watched: List<WatchedMovie> = emptyList(),
        myList: List<MyListItem> = emptyList(),
        heroEnabled: Boolean = true
    ) {
        val catalogue = mockk<GetMoviesByCategoryPaginatedUseCase>()
        every { catalogue(any()) } returns flowOf(PagingData.from(listOf(testMovie("1"), testMovie("2"))))
        val history = mockk<ObserveWatchHistoryUseCase>()
        every { history() } returns flowOf(Result.Success(watched))
        val list = mockk<ObserveMyListUseCase>()
        every { list() } returns flowOf(Result.Success(myList))
        val flags = mockk<ObserveFeatureFlagsUseCase>()
        every { flags() } returns flowOf(FeatureFlags(showHeroBanner = heroEnabled))
        val viewModel = HomeViewModel(catalogue, history, list, flags, mockk<RemoveFromWatchHistoryUseCase>())
        composeRule.setContent {
            HomeScreen(
                onMovieClick = { opened = it },
                onPlayClick = { played = it },
                onSearchClick = {},
                viewModel = viewModel
            )
        }
    }

    @Test
    fun `shows a hero for the first trending title and a row per category`() {
        setScreen()

        composeRule.onNodeWithText("Play trailer").assertIsDisplayed()
        composeRule.onNodeWithText("Trending Now").assertIsDisplayed()
        composeRule.onNodeWithText("Top Rated").assertExists()
    }

    @Test
    fun `the hero can be switched off remotely`() {
        setScreen(heroEnabled = false)

        composeRule.onNodeWithText("Play trailer").assertDoesNotExist()
        composeRule.onNodeWithText("Trending Now").assertIsDisplayed()
    }

    @Test
    fun `the hero buttons open the player and the detail page`() {
        setScreen()

        composeRule.onNodeWithText("Play trailer").performClick()
        composeRule.onNodeWithText("Details").performClick()
        composeRule.waitForIdle()

        assertEquals("1", played)
        assertEquals("1", opened)
    }

    @Test
    fun `library rows only appear when they have content`() {
        setScreen()
        composeRule.onNodeWithText("Continue watching").assertDoesNotExist()
        composeRule.onNodeWithText("My List").assertDoesNotExist()
    }

    @Test
    fun `continue watching and my list rows are shown`() {
        setScreen(
            watched = listOf(WatchedMovie("9", "Heat", null, null, 1, 5_000)),
            myList = listOf(MyListItem("8", "Alien", null, 1))
        )

        composeRule.onNodeWithText("Continue watching").assertIsDisplayed()
        composeRule.onNodeWithText("Heat").assertIsDisplayed()
        composeRule.onNodeWithText("My List").assertIsDisplayed()
    }
}
