package com.asadraza.streamflix.feature.search

import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie

/**
 * Contract for Search screen
 *
 * User searches for movies
 * Search screen
 */

data class SearchState(
    val query: String = "",
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedGenres: Set<Genre> = emptySet(),
    val searchHistory: List<String> = emptyList()
)

sealed class SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent()
    data class OnMovieClick(val movieId: String) : SearchEvent()
    data class OnGenreToggle(val genre: Genre) : SearchEvent()
    object OnClearSearch : SearchEvent()
    object OnBackPressed : SearchEvent()
    data class OnHistoryItemClick(val query: String) : SearchEvent()
}

sealed class SearchEffect {
    data class NavigateToDetail(val movieId: String) : SearchEffect()
    object NavigateBack : SearchEffect()
}