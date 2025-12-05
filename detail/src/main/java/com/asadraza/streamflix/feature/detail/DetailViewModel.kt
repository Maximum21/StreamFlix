package com.asadraza.streamflix.feature.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.home.GetMovieDetailsUseCase
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
 * ViewModel for Movie detail screen
 * Delegates to GetMovieDetailsUseCase
 *
 * @param savedStateHandle For retrieving movieId from navigation args
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: String = savedStateHandle.get("movieId") ?: ""

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effect = Channel<DetailEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadMovieDetails()
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.OnBackClick -> handleBackClick()
            DetailEvent.OnPlayTrailer -> handlePlayTrailer()
            DetailEvent.OnRetry -> loadMovieDetails()
        }
    }

    private fun loadMovieDetails() {
        viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                movieDetail = result.data,
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

    private fun handleBackClick() {
        viewModelScope.launch {
            _effect.send(DetailEffect.NavigateBack)
        }
    }

    private fun handlePlayTrailer() {
        viewModelScope.launch {
            _effect.send(DetailEffect.NavigateToPlayer(movieId))
        }
    }
}