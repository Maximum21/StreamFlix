package com.asadraza.streamflix.core.domain.usecase.home

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.model.movie.Movie
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
 * Use case for searching movies
 *
 * only needs search operations
 * test: mock only what's needed
 */
@OptIn(FlowPreview::class)
class SearchMoviesUseCase @Inject constructor(
    private val searchRepository: MovieSearchRepository
) {
    /**
     * Search with automatic debouncing and validation
     *
     * HOW IT WORKS:
     * 1. Filter out queries shorter than 3 chars
     * 2. Debounce by 500ms (user stopped typing)
     * 3. Only search if query actually changed
     * 4. Cancel previous search if new query comes
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(queryFlow: Flow<String>): Flow<Result<List<Movie>>> {
        return queryFlow
            .filter { it.length >= Constants.SEARCH_MIN_LENGTH }
            .debounce(Constants.SEARCH_DEBOUNCE_MILLIS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(Result.Success(emptyList()))
                } else {
                    searchRepository.searchMovies(query)
                }
            }
    }
}