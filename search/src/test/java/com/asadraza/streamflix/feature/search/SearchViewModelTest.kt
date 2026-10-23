package com.asadraza.streamflix.feature.search

import androidx.paging.PagingData
import app.cash.turbine.test
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.DiscoverMoviesByGenreUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.SearchMoviesPaginatedUseCase
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.testing.MainDispatcherRule
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchMovies = mockk<SearchMoviesPaginatedUseCase>()
    private val discoverByGenre = mockk<DiscoverMoviesByGenreUseCase>()
    private val history = MutableStateFlow<List<String>>(emptyList())
    private val observeHistory = mockk<ObserveSearchHistoryUseCase>().also { every { it() } returns history }
    private val addQuery = mockk<AddSearchQueryUseCase>(relaxed = true)
    private val removeQuery = mockk<RemoveSearchQueryUseCase>(relaxed = true)
    private val clearHistory = mockk<ClearSearchHistoryUseCase>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private fun createViewModel(): SearchViewModel {
        every { searchMovies(any()) } returns flowOf(PagingData.empty())
        every { discoverByGenre(any()) } returns flowOf(PagingData.empty())
        return SearchViewModel(
            searchMovies,
            discoverByGenre,
            SearchHistoryUseCases(observeHistory, addQuery, removeQuery, clearHistory),
            analytics
        )
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
        // The empty query was searched without waiting for the debounce
        verify(exactly = 1) { searchMovies("") }
        collector.cancel()
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
    fun `picking a genre leaves text search and browses on the server only`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }
        viewModel.onEvent(SearchEvent.OnQueryChange("batman"))
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()
        verify(exactly = 1) { searchMovies("batman") }

        viewModel.onEvent(SearchEvent.OnGenreToggle(Genre(12, "Adventure")))
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()

        assertEquals("", viewModel.state.value.query)
        assertEquals(setOf(Genre(12, "Adventure")), viewModel.state.value.selectedGenres)
        verify(exactly = 1) { discoverByGenre(setOf(12)) }
        // The old combined behaviour fetched the query's results again to filter them on the phone
        verify(exactly = 1) { searchMovies("batman") }
        collector.cancel()
    }

    @Test
    fun `typing a query leaves genre browsing`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(SearchEvent.OnGenreToggle(Genre(12, "Adventure")))

        viewModel.onEvent(SearchEvent.OnQueryChange("heat"))

        assertTrue(viewModel.state.value.selectedGenres.isEmpty())
        assertEquals("heat", viewModel.state.value.query)
    }

    @Test
    fun `clearing the last genre returns to an empty search`() = runTest {
        val viewModel = createViewModel()
        val action = Genre(28, "Action")
        viewModel.onEvent(SearchEvent.OnGenreToggle(action))

        viewModel.onEvent(SearchEvent.OnGenreToggle(action))

        assertTrue(viewModel.state.value.selectedGenres.isEmpty())
        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `picking genres browses by genre instead of searching`() = runTest {
        val viewModel = createViewModel()
        val collector = launch { viewModel.searchResults.collect {} }
        runCurrent()

        viewModel.onEvent(SearchEvent.OnGenreToggle(Genre(28, "Action")))
        viewModel.onEvent(SearchEvent.OnGenreToggle(Genre(12, "Adventure")))
        runCurrent()

        verify { discoverByGenre(setOf(28, 12)) }
        verify(exactly = 0) { searchMovies("batman") }
        collector.cancel()
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
