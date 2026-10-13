package com.asadraza.streamflix.feature.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.usecase.home.SearchMoviesPaginatedUseCase
import com.asadraza.streamflix.core.domain.usecase.home.SearchMoviesUseCase
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search screen with Paging 3 support
 *
 * Features:
 * - Paginated search results with infinite scrolling
 * - Debounced search (500ms)
 * - Search history tracking
 * - Genre filtering support
 *
 * Uses MVI pattern with:
 * - State: SearchState for UI state
 * - Events: SearchEvent for user actions
 * - Effects: SearchEffect for navigation/toasts
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val searchMoviesPaginatedUseCase: SearchMoviesPaginatedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effect = Channel<SearchEffect>()
    val effect = _effect.receiveAsFlow()

    // Query flow for pagination
    private val queryFlow = MutableStateFlow("")

    /**
     * Paginated search results
     *
     * Automatically debounced and filtered by SearchMoviesPaginatedUseCase
     * Cached in viewModelScope to survive configuration changes
     */
    val searchResults: Flow<PagingData<Movie>> = queryFlow
        .filter { it.length >= Constants.SEARCH_MIN_LENGTH }
        .debounce(Constants.SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                searchMoviesPaginatedUseCase.searchImmediate(query, viewModelScope)
            }
        }
        .cachedIn(viewModelScope)

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> handleQueryChange(event.query)
            is SearchEvent.OnMovieClick -> handleMovieClick(event.movieId)
            is SearchEvent.OnGenreToggle -> handleGenreToggle(event.genre)
            SearchEvent.OnClearSearch -> handleClearSearch()
            SearchEvent.OnBackPressed -> handleBackClick()
            is SearchEvent.OnHistoryItemClick -> handleHistoryItemClick(event.query)
        }
    }

    private fun handleQueryChange(query: String) {
        _state.update { it.copy(query = query, isLoading = query.length >= Constants.SEARCH_MIN_LENGTH) }
        queryFlow.value = query
    }

    private fun handleMovieClick(movieId: String) {
        // Save to history
        addToHistory(_state.value.query)

        viewModelScope.launch {
            _effect.send(SearchEffect.NavigateToDetail(movieId))
        }
    }

    private fun handleBackClick() {
        viewModelScope.launch {
            _effect.send(SearchEffect.NavigateBack)
        }
    }

    private fun handleGenreToggle(genre: Genre) {
        _state.update { state ->
            val newGenres = if (genre in state.selectedGenres) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newGenres)
        }
    }

    private fun handleClearSearch() {
        _state.update { it.copy(query = "", isLoading = false) }
        queryFlow.value = ""
    }

    private fun handleHistoryItemClick(query: String) {
        handleQueryChange(query)
    }

    private fun addToHistory(query: String) {
        if (query.isBlank()) return

        _state.update { state ->
            val newHistory = (listOf(query) + state.searchHistory)
                .distinct()
                .take(10) // Keep last 10 searches
            state.copy(searchHistory = newHistory)
        }
    }
}