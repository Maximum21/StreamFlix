package com.asadraza.streamflix.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MyListViewModel @Inject constructor(private val observeMyList: ObserveMyListUseCase) : ViewModel() {
    private val _state = MutableStateFlow(MyListState())
    val state: StateFlow<MyListState> = _state.asStateFlow()

    private val _effect = Channel<MyListEffect>(Channel.BUFFERED)
    val effect: Flow<MyListEffect> = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: MyListEvent) {
        when (event) {
            is MyListEvent.OnItemClick -> viewModelScope.launch {
                _effect.send(
                    MyListEffect.NavigateToDetail(event.movieId)
                )
            }
            MyListEvent.OnRetry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            observeMyList().collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true, errorType = null) }
                    is Result.Success -> _state.update {
                        it.copy(items = result.data, isLoading = false, errorType = null)
                    }
                    // Keep the last list on screen when a refresh fails
                    is Result.Error -> _state.update {
                        it.copy(items = result.data ?: it.items, isLoading = false, errorType = result.type)
                    }
                }
            }
        }
    }
}
