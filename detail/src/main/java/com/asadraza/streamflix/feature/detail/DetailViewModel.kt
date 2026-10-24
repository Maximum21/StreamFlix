package com.asadraza.streamflix.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.library.AddToMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveIsInMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromMyListUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Movie detail screen
 * Delegates to GetMovieDetailsUseCase
 *
 * @param savedStateHandle For retrieving movieId from navigation args
 */
@HiltViewModel
class DetailViewModel
@Inject
constructor(
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    observeIsInMyList: ObserveIsInMyListUseCase,
    private val addToMyList: AddToMyListUseCase,
    private val removeFromMyList: RemoveFromMyListUseCase,
    private val analytics: AnalyticsTracker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val movieId: String = savedStateHandle.get("movieId") ?: ""

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effect = Channel<DetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val isInMyList: StateFlow<Boolean> =
        observeIsInMyList(movieId).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var loadJob: Job? = null

    init {
        loadMovieDetails()
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.OnBackClick -> handleBackClick()
            DetailEvent.OnPlayTrailer -> handlePlayTrailer()
            DetailEvent.OnRetry -> loadMovieDetails()
            DetailEvent.OnToggleMyList -> toggleMyList()
            is DetailEvent.OnSimilarClick -> viewModelScope.launch {
                _effect.send(DetailEffect.NavigateToDetail(event.movieId))
            }
        }
    }

    private fun loadMovieDetails() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                _state.value =
                    when (result) {
                        is Result.Loading ->
                            // keep showing content if we already have it (offline-first); else Loading
                            (_state.value as? DetailState.Success) ?: DetailState.Loading
                        is Result.Success ->
                            DetailState.Success(movieDetail = result.data)
                        is Result.Error ->
                            // if we already have content, keep it; else show error
                            (_state.value as? DetailState.Success)
                                ?: DetailState.Error(result.type)
                    }
            }
        }
    }

    private fun toggleMyList() {
        val detail = (_state.value as? DetailState.Success)?.movieDetail ?: return
        analytics.log(AnalyticsEvent.MyListChanged(detail.id, added = !isInMyList.value))
        val changes = if (isInMyList.value) {
            removeFromMyList(detail.id)
        } else {
            addToMyList(MyListItem(detail.id, detail.title, detail.posterPath, System.currentTimeMillis()))
        }
        viewModelScope.launch {
            changes.collect { result ->
                if (result is Result.Error) _effect.send(DetailEffect.ShowMessage(result.type.toUiText()))
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
