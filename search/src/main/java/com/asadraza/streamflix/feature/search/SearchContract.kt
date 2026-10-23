package com.asadraza.streamflix.feature.search

import com.asadraza.streamflix.core.model.movie.Genre

data class SearchState(
    val query: String = "",
    val selectedGenres: Set<Genre> = emptySet(),
    val searchHistory: List<String> = emptyList()
)

sealed class SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent()

    data class OnMovieClick(val movieId: String) : SearchEvent()

    data class OnGenreToggle(val genre: Genre) : SearchEvent()

    data object OnClearSearch : SearchEvent()

    data class OnHistoryItemClick(val query: String) : SearchEvent()

    data class OnHistoryItemRemove(val query: String) : SearchEvent()

    data object OnClearHistory : SearchEvent()
}

sealed class SearchEffect {
    data class NavigateToDetail(val movieId: String) : SearchEffect()
}
