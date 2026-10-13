package com.asadraza.streamflix.feature.home

import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory

/**
 * Contract for Home screen (MVI pattern)
 */

/**
 * Represents the UI state
 */
data class HomeState(
    val movieCategories: Map<MovieCategory, CategoryState> = emptyMap(),
    val isOffline: Boolean = false,
    val selectedCategory: MovieCategory = MovieCategory.Trending
) {
    /**
     * Category-specific state
     */
    data class CategoryState(
        val movies: List<Movie> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
}

/**
 * User interactions
 */
sealed class HomeEvent {
    data class OnMovieClick(val movieId: String) : HomeEvent()
    data class OnCategorySelect(val category: MovieCategory) : HomeEvent()
    object OnRefresh : HomeEvent()
    object OnSearchClick : HomeEvent()
    object OnRetryConnection : HomeEvent()
}

/**
 * One-time UI events
 * NavigateToLogin for auth errors
 */
sealed class HomeEffect {
    data class NavigateToDetail(val movieId: String) : HomeEffect()
    data class ShowToast(val message: String) : HomeEffect()
    object NavigateToLogin : HomeEffect()
    object NavigateToSearch : HomeEffect()
}