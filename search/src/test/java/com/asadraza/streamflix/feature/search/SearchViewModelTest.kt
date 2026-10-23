package com.asadraza.streamflix.feature.search

import androidx.paging.PagingData
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.SearchMoviesPaginatedUseCase
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import com.asadraza.streamflix.core.testing.testMovie
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchMovies = mockk<SearchMoviesPaginatedUseCase>()
    private val history = MutableStateFlow<List<String>>(emptyList())
    private val observeHistory = mockk<ObserveSearchHistoryUseCase>().also { every { it() } returns history }
    private val addQuery = mockk<AddSearchQueryUseCase>(relaxed = true)
    private val removeQuery = mockk<RemoveSearchQueryUseCase>(relaxed = true)
    private val clearHistory = mockk<ClearSearchHistoryUseCase>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private fun createViewModel(): SearchViewModel {
        every { searchMovies(any()) } returns flowOf(PagingData.empty())
        return SearchViewModel(searchMovies, observeHistory, addQuery, removeQuery, clearHistory, analytics)
    }

    @Test
    fun `typing is debounced and only the last query is searched`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }

        listOf("b", "ba", "bat").forEach { viewModel.onEvent(SearchEvent.OnQueryChange(it)) }
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS - 1)
        runCurrent()
        verify(exactly = 0) { searchMovies("bat") }

        advanceTimeBy(2)
        runCurrent()
        verify(exactly = 1) { searchMovies("bat") }
        verify(exactly = 0) { searchMovies("b") }
        verify(exactly = 0) { searchMovies("ba") }
        collector.cancel()
    }

    @Test
    fun `the query is trimmed before searching`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }

        viewModel.onEvent(SearchEvent.OnQueryChange("  bat  "))
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()

        verify { searchMovies("bat") }
        collector.cancel()
    }

    @Test
    fun `clearing the search resets results immediately`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }
        viewModel.onEvent(SearchEvent.OnQueryChange("batman"))
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()

        viewModel.onEvent(SearchEvent.OnClearSearch)
        runCurrent()

        assertEquals("", viewModel.state.value.query)
        assertFalse(viewModel.state.value.isLoading)
        // The empty query was searched without waiting for the debounce
        verify(exactly = 1) { searchMovies("") }
        collector.cancel()
    }

    @Test
    fun `short queries do not show a loading state`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SearchEvent.OnQueryChange("ba"))
        assertFalse(viewModel.state.value.isLoading)

        viewModel.onEvent(SearchEvent.OnQueryChange("bat"))
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `opening a result saves the query to history and navigates`() = runTest {
        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.onEvent(SearchEvent.OnQueryChange("batman"))
            viewModel.onEvent(SearchEvent.OnMovieClick("1"))
            assertEquals(SearchEffect.NavigateToDetail("1"), awaitItem())
        }

        coVerify { addQuery("batman") }
        verify { analytics.log(AnalyticsEvent.SearchResultOpened(queryLength = 6)) }
    }

    @Test
    fun `history from the repository is exposed and can be edited`() = runTest {
        val viewModel = createViewModel()

        history.value = listOf("heat", "alien")
        runCurrent()
        assertEquals(listOf("heat", "alien"), viewModel.state.value.searchHistory)

        viewModel.onEvent(SearchEvent.OnHistoryItemRemove("heat"))
        viewModel.onEvent(SearchEvent.OnClearHistory)
        runCurrent()
        coVerify { removeQuery("heat") }
        coVerify { clearHistory() }
    }

    @Test
    fun `picking a history entry searches for it`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SearchEvent.OnHistoryItemClick("heat"))

        assertEquals("heat", viewModel.state.value.query)
    }

    @Test
    fun `changing the genre filter starts a new search`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }
        viewModel.onEvent(SearchEvent.OnQueryChange("batman"))
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()
        verify(exactly = 1) { searchMovies("batman") }

        viewModel.onEvent(SearchEvent.OnGenreToggle(Genre(28, "Action")))
        runCurrent()

        verify(exactly = 2) { searchMovies("batman") }
        collector.cancel()
    }

    @Test
    fun `genre matching`() {
        val action = testMovie("1").copy(genreIds = listOf(28, 12))

        assertTrue(action.matchesAnyGenre(emptySet()))
        assertTrue(action.matchesAnyGenre(setOf(12)))
        assertTrue(action.matchesAnyGenre(setOf(35, 28)))
        assertFalse(action.matchesAnyGenre(setOf(35)))
    }

    @Test
    fun `genres toggle on and off`() {
        val viewModel = createViewModel()
        val action = Genre(28, "Action")

        viewModel.onEvent(SearchEvent.OnGenreToggle(action))
        assertEquals(setOf(action), viewModel.state.value.selectedGenres)

        viewModel.onEvent(SearchEvent.OnGenreToggle(action))
        assertTrue(viewModel.state.value.selectedGenres.isEmpty())
    }
}
