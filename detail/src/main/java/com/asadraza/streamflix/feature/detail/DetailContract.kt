package com.asadraza.streamflix.feature.detail

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.ui.text.UiText

/**
 * Contract for Detail screen (MVI pattern)
 *
 */

sealed interface DetailState {
    data object Loading : DetailState

    data class Success(val movieDetail: MovieDetail) : DetailState

    data class Error(val errorType: ErrorType) : DetailState
}

sealed class DetailEvent {
    data object OnBackClick : DetailEvent()

    data object OnPlayTrailer : DetailEvent()

    data object OnRetry : DetailEvent()

    data object OnToggleMyList : DetailEvent()

    data class OnSimilarClick(val movieId: String) : DetailEvent()
}

sealed class DetailEffect {
    data object NavigateBack : DetailEffect()

    data class NavigateToPlayer(val movieId: String) : DetailEffect()

    data class NavigateToDetail(val movieId: String) : DetailEffect()

    data class ShowMessage(val message: UiText) : DetailEffect()
}
