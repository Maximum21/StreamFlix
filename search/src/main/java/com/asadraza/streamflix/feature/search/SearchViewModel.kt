package com.asadraza.streamflix.feature.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.home.SearchMoviesUseCase
import com.asadraza.streamflix.core.model.movie.Genre
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search screen
 *
 * Handles search with proper debounce
 * User types in search box
 * Delegates to SearchMoviesUseCase (which has debounce logic)
 *
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effect = Channel<SearchEffect>()
    val effect = _effect.receiveAsFlow()

    // Query flow for UseCase
    private val queryFlow = MutableStateFlow("")

    init {
        observeSearchResults()
    }

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

    /**
     * Observe search results from UseCase
     * UseCase handles debounce, we just collect results
     */
    private fun observeSearchResults() {
        viewModelScope.launch {
            searchMoviesUseCase(queryFlow).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        Log.e("searcgmivue","==result=${Gson().toJson(result.data)}")
                        _state.update {
                            it.copy(
                                movies = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleQueryChange(query: String) {
        _state.update { it.copy(query = query) }
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
        _state.update { it.copy(query = "", movies = emptyList()) }
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