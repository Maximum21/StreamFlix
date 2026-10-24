package com.asadraza.streamflix.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.usecase.search.DiscoverMoviesByGenreUseCase
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
 * Search with paged results and a persisted history, plus browsing by genre. The two are separate
 * modes (TMDB's search endpoint has no genre parameter), so choosing one clears the other.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val searchMoviesPaginatedUseCase: SearchMoviesPaginatedUseCase,
    private val discoverMoviesByGenre: DiscoverMoviesByGenreUseCase,
    private val history: SearchHistoryUseCases,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private val genreFlow = MutableStateFlow<Set<Genre>>(emptySet())

    // Debounce typing, but clear stale results straight away when the field is emptied. The genre
    // selection is part of the upstream key: a PagingData can only be transformed and collected once,
    // so changing it has to start a new load rather than re-filter an old one.
    val searchResults: Flow<PagingData<Movie>> =
        combine(
            queryFlow
                .debounce { if (it.isBlank()) 0L else Constants.SEARCH_DEBOUNCE_MILLIS }
                .map { it.trim() }
                .distinctUntilChanged(),
            genreFlow
        ) { query, genres -> sourceFor(query, genres.map { it.id }.toSet()) }
            // Changing the text and the genres together arrives as two updates; only a different
            // source should start a new load, otherwise the same list is requested twice
            .distinctUntilChanged()
            .flatMapLatest { source -> resultsFor(source) }
            .cachedIn(viewModelScope)

    // Text search and genre browsing are separate modes, and the ViewModel keeps them exclusive. TMDB's
    // search endpoint cannot filter by genre, so combining them would mean filtering results on the
    // phone: few matches per page, so page after page is fetched and mostly discarded. Genres win if
    // both are momentarily set while one change is applied.
    private fun sourceFor(query: String, genreIds: Set<Int>): ResultSource =
        if (genreIds.isNotEmpty()) ResultSource.Genres(genreIds) else ResultSource.Query(query)

    private fun resultsFor(source: ResultSource): Flow<PagingData<Movie>> = when (source) {
        is ResultSource.Genres -> discoverMoviesByGenre(source.ids)
        is ResultSource.Query -> searchMoviesPaginatedUseCase(source.text)
    }

    /** Where the current list comes from. Equal sources are the same list, so they are not reloaded. */
    private sealed interface ResultSource {
        data class Genres(val ids: Set<Int>) : ResultSource

        data class Query(val text: String) : ResultSource
    }

    init {
        history.observe()
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
            is SearchEvent.OnHistoryItemRemove -> viewModelScope.launch { history.remove(event.query) }
            SearchEvent.OnClearHistory -> viewModelScope.launch { history.clear() }
        }
    }

    private fun handleQueryChange(query: String) {
        // Typing leaves genre browsing
        val genres = if (query.isNotBlank()) emptySet() else _state.value.selectedGenres
        _state.update { it.copy(query = query, selectedGenres = genres) }
        genreFlow.value = genres
        queryFlow.value = query
    }

    // A query counts as meaningful once the user opens one of its results
    private fun handleMovieClick(movieId: String) {
        val query = _state.value.query
        analytics.log(AnalyticsEvent.SearchResultOpened(query.length))
        viewModelScope.launch {
            history.add(query)
            _effect.send(SearchEffect.NavigateToDetail(movieId))
        }
    }

    private fun handleGenreToggle(genre: Genre) {
        val updated = _state.value.selectedGenres.let { if (genre in it) it - genre else it + genre }
        // Picking a genre leaves text search
        val query = if (updated.isNotEmpty()) "" else _state.value.query
        _state.update { it.copy(selectedGenres = updated, query = query) }
        genreFlow.value = updated
        queryFlow.value = query
    }
}
