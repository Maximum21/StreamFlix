package com.asadraza.streamflix.feature.search

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.asadraza.streamflix.core.domain.repository.MovieDiscoverRepository
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.DiscoverMoviesByGenreUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.SearchMoviesPaginatedUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen() {
        // The real use case is used on purpose: the empty result it returns for short queries is
        // what the screen renders when it is first opened
        val viewModel = SearchViewModel(
            SearchMoviesPaginatedUseCase(mockk<MovieSearchRepository>()),
            DiscoverMoviesByGenreUseCase(mockk<MovieDiscoverRepository>()),
            SearchHistoryUseCases(
                mockk<ObserveSearchHistoryUseCase>().also { every { it() } returns flowOf(emptyList()) },
                mockk<AddSearchQueryUseCase>(relaxed = true),
                mockk<RemoveSearchQueryUseCase>(relaxed = true),
                mockk<ClearSearchHistoryUseCase>(relaxed = true)
            ),
            mockk(relaxed = true)
        )
        composeRule.setContent { SearchScreen(onMovieClick = {}, viewModel = viewModel) }
    }

    @Test
    fun `opening search shows the prompt instead of a spinner that never ends`() {
        setScreen()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Enter at least 3 characters to search").assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }
}
