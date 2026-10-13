package com.asadraza.streamflix.core.domain.usecase.home

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Use case for paginated movie search
 *
 * Uses Paging 3 for efficient infinite scrolling
 * Maintains same debounce behavior as non-paginated search
 *
 * KEY FEATURES:
 * - Debounced search (500ms)
 * - Minimum query length validation (3 chars)
 * - Automatic cancellation of previous searches
 * - Efficient memory management via Paging 3
 */
@OptIn(FlowPreview::class)
class SearchMoviesPaginatedUseCase @Inject constructor(
    private val searchRepository: MovieSearchRepository
) {
    /**
     * Search with pagination and automatic debouncing
     *
     * HOW IT WORKS:
     * 1. Filter out queries shorter than 3 chars
     * 2. Debounce by 500ms (user stopped typing)
     * 3. Only search if query actually changed
     * 4. Cancel previous search if new query comes
     * 5. Return PagingData for efficient scrolling
     *
     * @param queryFlow Flow of search queries
     * @param scope CoroutineScope for caching (typically viewModelScope)
     * @return Flow of PagingData<Movie>
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        queryFlow: Flow<String>,
        scope: CoroutineScope
    ): Flow<PagingData<Movie>> {
        return queryFlow
            .filter { it.length >= Constants.SEARCH_MIN_LENGTH }
            .debounce(Constants.SEARCH_DEBOUNCE_MILLIS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(PagingData.empty())
                } else {
                    searchRepository.searchMoviesPaginated(query)
                        .cachedIn(scope)
                }
            }
    }

    /**
     * Direct search without debounce (for immediate search on button press)
     *
     * @param query Search query
     * @param scope CoroutineScope for caching
     * @return Flow of PagingData<Movie>
     */
    fun searchImmediate(query: String, scope: CoroutineScope): Flow<PagingData<Movie>> {
        return if (query.length < Constants.SEARCH_MIN_LENGTH) {
            flowOf(PagingData.empty())
        } else {
            searchRepository.searchMoviesPaginated(query)
                .cachedIn(scope)
        }
    }
}