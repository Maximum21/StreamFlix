package com.asadraza.streamflix.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RecordWatchUseCase
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.movie.MovieDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Resolves the movie's official YouTube trailer. Playback itself is owned by the YouTube player
 * view, so the ViewModel holds no player state.
 */
@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val observeWatchHistory: ObserveWatchHistoryUseCase,
    private val recordWatch: RecordWatchUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val movieId: String = savedStateHandle["movieId"] ?: ""

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _effect = Channel<PlayerEffect>(Channel.BUFFERED)
    val effect: Flow<PlayerEffect> = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var detail: MovieDetail? = null
    private var lastRecordedMillis = NOT_RECORDED

    init {
        loadTrailer()
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.OnBackPressed -> viewModelScope.launch { _effect.send(PlayerEffect.NavigateBack) }
            PlayerEvent.OnRetry -> loadTrailer()
            is PlayerEvent.OnPositionChanged -> recordIfDue(event.positionMillis)
            is PlayerEvent.OnPlaybackStopped -> record(event.positionMillis)
        }
    }

    private fun loadTrailer() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true, errorType = null) }
                    is Result.Success -> {
                        detail = result.data
                        val resumeAt = resumePositionMillis()
                        _state.update {
                            it.copy(
                                movieTitle = result.data.title,
                                trailerKey = result.data.officialTrailer?.key,
                                resumeAtMillis = resumeAt,
                                isLoading = false,
                                errorType = null
                            )
                        }
                    }
                    is Result.Error -> _state.update { it.copy(isLoading = false, errorType = result.type) }
                }
            }
        }
    }

    private suspend fun resumePositionMillis(): Long {
        val history = observeWatchHistory().firstOrNull { it !is Result.Loading } as? Result.Success
        return history?.data?.firstOrNull { it.movieId == movieId }?.positionMillis ?: 0L
    }

    // The player reports far more often than is worth a network write
    private fun recordIfDue(positionMillis: Long) {
        // The first position report means the trailer actually started playing
        if (lastRecordedMillis == NOT_RECORDED) detail?.let { analytics.log(AnalyticsEvent.TrailerPlayed(it.id)) }
        if (lastRecordedMillis == NOT_RECORDED || abs(positionMillis - lastRecordedMillis) >= RECORD_INTERVAL_MILLIS) {
            record(positionMillis)
        }
    }

    private fun record(positionMillis: Long) {
        val movie = detail ?: return
        lastRecordedMillis = positionMillis
        viewModelScope.launch {
            recordWatch(
                WatchedMovie(
                    movieId = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    watchedAt = System.currentTimeMillis(),
                    positionMillis = positionMillis
                )
            )
        }
    }

    private companion object {
        const val NOT_RECORDED = -1L
        const val RECORD_INTERVAL_MILLIS = 10_000L
    }
}
