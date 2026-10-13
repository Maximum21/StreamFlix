package com.asadraza.streamflix.feature.detail

import com.asadraza.streamflix.core.model.movie.MovieDetail

/**
 * Contract for Detail screen (MVI pattern)
 *
 */

sealed interface DetailState {
    data object Loading : DetailState
    data class Success(
        val movieDetail: MovieDetail,          // ← non-null, guaranteed present here
        val isTrailerPlaying: Boolean = false
    ) : DetailState
    data class Error(val message: String) : DetailState
}

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