package com.asadraza.streamflix.feature.detail

import com.asadraza.streamflix.core.model.movie.MovieDetail

/**
 * Contract for Detail screen (MVI pattern)
 *
 * WHY: Consistent architecture across all features
 * WHEN: User taps on a movie
 * WHERE: Detail screen
 */

data class DetailState(
    val movieDetail: MovieDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTrailerPlaying: Boolean = false
)

sealed class DetailEvent {
    object OnBackClick : DetailEvent()
    object OnPlayTrailer : DetailEvent()
    object OnRetry : DetailEvent()
}

sealed class DetailEffect {
    object NavigateBack : DetailEffect()
    data class NavigateToDetail(val movieId: String) : DetailEffect()
    data class NavigateToPlayer(val movieId: String) : DetailEffect()
    data class ShowToast(val message: String) : DetailEffect()
}