package com.asadraza.streamflix.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.home.GetMovieDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
//    private val getMovieStreamUrlUseCase: GetMovieStreamUrlUseCase
): ViewModel(){
    private val movieId = savedStateHandle["movieId"] ?: ""

    private val _state = MutableStateFlow(PlayerState(movieId))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _effect = Channel<PlayerEffect>()
    val effect: Flow<PlayerEffect> = _effect.receiveAsFlow()

    private var hideControlsJob : Job? = null

    init {
        loadMovieDetails()
//        loadStreamUrl() //Used when have multiple urls for multiple qualities.
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.OnPlayPauseClick -> togglePlayPause()
            is PlayerEvent.OnPlay -> onPlay()
            is PlayerEvent.OnPause -> onPause()
            is PlayerEvent.OnSeek -> onSeek(event.position)
            is PlayerEvent.OnRewind -> onRewind()
            is PlayerEvent.OnForward -> onForward()
            is PlayerEvent.OnScreenTap -> toggleControls()
            is PlayerEvent.OnFullscreenToggle -> toggleFullscreen()
            is PlayerEvent.OnQualityClick -> toggleQualitySelector()
            is PlayerEvent.OnQualitySelected -> selectQuality(event.quality)
            is PlayerEvent.OnSpeedClick -> toggleSpeedSelector()
            is PlayerEvent.OnSpeedSelected -> selectSpeed(event.speed)
            is PlayerEvent.OnBackPressed -> navigateBack()
            is PlayerEvent.OnBuffering -> onBuffering()
            is PlayerEvent.OnReady -> onReady()
            is PlayerEvent.OnVideoEnded -> onVideoEnded()
            is PlayerEvent.OnRetry -> retry()
            is PlayerEvent.OnErrorDismiss -> dismissError()
        }
    }

    fun loadMovieDetails(){
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getMovieDetailsUseCase.invoke(movieId).collect { result->
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                movieTitle = result.data.title,
                                videoType = "youtube",
                                streamUrl = if(result.data.videos.isNotEmpty()) result.data.videos[1].key else "",
                                isLoading = false
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorType = result.type
                            )
                        }
                    }
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
/**
 * Method that can be used to fetch streaming url for movie with different video qualities.
 * */
//    private fun loadStreamUrl() {
//        viewModelScope.launch {
//            _state.update { it.copy(isLoading = true) }
//
//            val quality = when (_state.value.currentQuality) {
//                is VideoQuality.SD -> "480p"
//                is VideoQuality.HD -> "720p"
//                is VideoQuality.FullHD -> "1080p"
//                is VideoQuality.UHD -> "2160p"
//                else -> "auto"
//            }
//
//            getMovieStreamUrlUseCase.invoke(movieId, quality).collect{result->
//                when (result) {
//                    is Result.Success -> {
//                        _state.update {
//                            it.copy(
//                                streamUrl = result.data,
//                                isLoading = false
//                            )
//                        }
//                    }
//                    is Result.Error -> {
//                        _state.update {
//                            it.copy(
//                                isLoading = false,
//                                errorType = result.type
//                            )
//                        }
//                        viewModelScope.launch {
//                            _effect.send(PlayerEffect.ShowError(result.type))
//                        }
//                    }
//                    is Result.Loading -> {
//                        _state.update { it.copy(isLoading = true) }
//                    }
//                }
//            }
//        }
//    }

    private fun togglePlayPause() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
        if (_state.value.isPlaying) {
            scheduleHideControls()
        }
    }

    private fun onPlay() {
        _state.update { it.copy(isPlaying = true) }
        scheduleHideControls()
    }

    private fun onPause() {
        _state.update { it.copy(isPlaying = false) }
        hideControlsJob?.cancel()
    }

    private fun onSeek(position: Long) {
        _state.update { it.copy(currentPosition = position) }
        scheduleHideControls()
    }

    private fun onRewind() {
        val newPosition = (_state.value.currentPosition - 10000).coerceAtLeast(0)
        _state.update { it.copy(currentPosition = newPosition) }
        scheduleHideControls()
    }

    private fun onForward() {
        val newPosition = (_state.value.currentPosition + 10000)
            .coerceAtMost(_state.value.duration)
        _state.update { it.copy(currentPosition = newPosition) }
        scheduleHideControls()
    }

    private fun toggleControls() {
        val showControls = !_state.value.showControls
        _state.update { it.copy(showControls = showControls) }

        if (showControls && _state.value.isPlaying) {
            scheduleHideControls()
        } else {
            hideControlsJob?.cancel()
        }
    }

    private fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    private fun toggleQualitySelector() {
        _state.update {
            it.copy(
                showQualitySelector = !it.showQualitySelector,
                showSpeedSelector = false
            )
        }
    }

    private fun selectQuality(quality: VideoQuality) {
        _state.update {
            it.copy(
                currentQuality = quality,
                showQualitySelector = false
            )
        }
//        loadStreamUrl() // Reload with new quality
    }

    private fun toggleSpeedSelector() {
        _state.update {
            it.copy(
                showSpeedSelector = !it.showSpeedSelector,
                showQualitySelector = false
            )
        }
    }

    private fun selectSpeed(speed: Float) {
        _state.update {
            it.copy(
                playbackSpeed = speed,
                showSpeedSelector = false
            )
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.send(PlayerEffect.NavigateBack)
        }
    }

    private fun onBuffering() {
        _state.update { it.copy(isLoading = true) }
    }

    private fun onReady() {
        _state.update { it.copy(isLoading = false) }
    }

    private fun onVideoEnded() {
        _state.update {
            it.copy(
                isPlaying = false,
                showControls = true
            )
        }
        hideControlsJob?.cancel()
    }

    private fun retry() {
        _state.update { it.copy(errorType = null) }
        loadMovieDetails()
//        loadStreamUrl()
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(3000)
            _state.update { it.copy(showControls = false) }
        }
    }

    fun updateProgress(position: Long, buffered: Long, duration: Long) {
        _state.update {
            it.copy(
                currentPosition = position,
                bufferedPosition = buffered,
                duration = duration
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        hideControlsJob?.cancel()
    }
}