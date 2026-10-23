package com.asadraza.streamflix.feature.player

import com.asadraza.streamflix.core.common.result.ErrorType

data class PlayerState(
    val movieTitle: String = "",
    // YouTube video id of the official trailer, null when TMDB lists none
    val trailerKey: String? = null,
    // Where the last session stopped, so playback can resume
    val resumeAtMillis: Long = 0L,
    val isLoading: Boolean = true,
    val errorType: ErrorType? = null
) {
    val hasNoTrailer: Boolean
        get() = !isLoading && errorType == null && trailerKey == null
}

sealed class PlayerEvent {
    data object OnBackPressed : PlayerEvent()

    data object OnRetry : PlayerEvent()

    /** Reported by the player roughly every half second while the trailer plays. */
    data class OnPositionChanged(val positionMillis: Long) : PlayerEvent()

    /** The screen is leaving, so the final position is saved regardless of the interval. */
    data class OnPlaybackStopped(val positionMillis: Long) : PlayerEvent()
}

sealed class PlayerEffect {
    data object NavigateBack : PlayerEffect()
}
