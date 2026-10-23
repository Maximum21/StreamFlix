package com.asadraza.streamflix.core.domain.usecase.search

import androidx.paging.PagingData
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.model.movie.Movie
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Searches for [query], returning no results for queries below the minimum length so the
 * network is never hit for them. Debouncing is a UI concern and stays in the ViewModel.
 */
class SearchMoviesPaginatedUseCase
@Inject
constructor(
    private val searchRepository: MovieSearchRepository
) {
    operator fun invoke(query: String): Flow<PagingData<Movie>> {
        val trimmed = query.trim()
        return if (trimmed.length < Constants.SEARCH_MIN_LENGTH) {
            flowOf(PagingData.empty())
        } else {
            searchRepository.searchMoviesPaginated(trimmed)
        }
    }
}
