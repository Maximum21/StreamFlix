package com.asadraza.streamflix.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.flags.ObserveFeatureFlagsUseCase
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryPaginatedUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromWatchHistoryUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for Home. Every category row is its own paged feed, cached so scrolling back and
 * rotating do not reload it. The library rows (continue watching, My List) come from the profile.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    getMoviesByCategoryPaginated: GetMoviesByCategoryPaginatedUseCase,
    observeWatchHistory: ObserveWatchHistoryUseCase,
    observeMyList: ObserveMyListUseCase,
    observeFeatureFlags: ObserveFeatureFlagsUseCase,
    private val removeFromWatchHistory: RemoveFromWatchHistoryUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    /** In display order. The flows are cold, so a row only loads once its composable is shown. */
    val categoryFeeds: Map<MovieCategory, Flow<PagingData<Movie>>> =
        MovieCategory.getAll().associateWith { category ->
            getMoviesByCategoryPaginated(category).cachedIn(viewModelScope)
        }

    init {
        // A failed library row simply stays empty; the catalogue rows are the main content
        observeWatchHistory()
            .onEach { result ->
                if (result is Result.Success) _state.update { it.copy(continueWatching = result.data) }
            }
            .launchIn(viewModelScope)
        observeFeatureFlags()
            .onEach { flags -> _state.update { it.copy(showHeroBanner = flags.showHeroBanner) } }
            .launchIn(viewModelScope)
        observeMyList()
            .onEach { result ->
                if (result is Result.Success) _state.update { it.copy(myList = result.data) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnMovieClick -> send(HomeEffect.NavigateToDetail(event.movieId))
            is HomeEvent.OnPlayClick -> send(HomeEffect.NavigateToPlayer(event.movieId))
            is HomeEvent.OnRemoveContinueWatching ->
                viewModelScope.launch { removeFromWatchHistory(event.movieId).collect {} }
            HomeEvent.OnSearchClick -> send(HomeEffect.NavigateToSearch)
        }
    }

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
