package com.asadraza.streamflix.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.SearchMoviesPaginatedUseCase
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Search with paged results, a persisted history and a genre filter. TMDB's search endpoint has no
 * genre parameter, so the filter is applied to the results on the client.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val searchMoviesPaginatedUseCase: SearchMoviesPaginatedUseCase,
    observeSearchHistory: ObserveSearchHistoryUseCase,
    private val addSearchQuery: AddSearchQueryUseCase,
    private val removeSearchQuery: RemoveSearchQueryUseCase,
    private val clearSearchHistory: ClearSearchHistoryUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effect = Channel<SearchEffect>()
    val effect = _effect.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private val genreFlow = MutableStateFlow<Set<Genre>>(emptySet())

    // Debounce typing, but clear stale results straight away when the field is emptied. The genre
    // filter is part of the upstream key: a PagingData can only be transformed and collected once,
    // so changing the filter has to start a new load rather than re-filter an old one.
    val searchResults: Flow<PagingData<Movie>> =
        combine(
            queryFlow
                .debounce { if (it.isBlank()) 0L else Constants.SEARCH_DEBOUNCE_MILLIS }
                .map { it.trim() }
                .distinctUntilChanged(),
            genreFlow
        ) { query, genres -> query to genres.map { it.id }.toSet() }
            .flatMapLatest { (query, genreIds) ->
                searchMoviesPaginatedUseCase(query).map { paging ->
                    if (genreIds.isEmpty()) {
                        paging
                    } else {
                        paging.filter { movie -> movie.matchesAnyGenre(genreIds) }
                    }
                }
            }
            .cachedIn(viewModelScope)

    init {
        observeSearchHistory()
            .onEach { history -> _state.update { it.copy(searchHistory = history) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> handleQueryChange(event.query)
            is SearchEvent.OnMovieClick -> handleMovieClick(event.movieId)
            is SearchEvent.OnGenreToggle -> handleGenreToggle(event.genre)
            SearchEvent.OnClearSearch -> handleQueryChange("")
            is SearchEvent.OnHistoryItemClick -> handleQueryChange(event.query)
            is SearchEvent.OnHistoryItemRemove -> viewModelScope.launch { removeSearchQuery(event.query) }
            SearchEvent.OnClearHistory -> viewModelScope.launch { clearSearchHistory() }
        }
    }

    private fun handleQueryChange(query: String) {
        _state.update { it.copy(query = query, isLoading = query.length >= Constants.SEARCH_MIN_LENGTH) }
        queryFlow.value = query
    }

    // A query counts as meaningful once the user opens one of its results
    private fun handleMovieClick(movieId: String) {
        val query = _state.value.query
        analytics.log(AnalyticsEvent.SearchResultOpened(query.length))
        viewModelScope.launch {
            addSearchQuery(query)
            _effect.send(SearchEffect.NavigateToDetail(movieId))
        }
    }

    private fun handleGenreToggle(genre: Genre) {
        val updated = _state.value.selectedGenres.let { if (genre in it) it - genre else it + genre }
        _state.update { it.copy(selectedGenres = updated) }
        genreFlow.value = updated
    }
}

/** True when no filter is set, or the movie belongs to at least one of the selected genres. */
internal fun Movie.matchesAnyGenre(genreIds: Set<Int>): Boolean =
    genreIds.isEmpty() || genreIds.any { it in this.genreIds }
