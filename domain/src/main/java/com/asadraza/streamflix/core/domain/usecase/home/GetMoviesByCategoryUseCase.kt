package com.asadraza.streamflix.core.domain.usecase.home

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting movies by category
 *
 * - only needs catalog operations
 * - test: mock only what's needed
 */
class GetMoviesByCategoryUseCase @Inject constructor(
    private val catalogRepository: MovieCatalogRepository
) {
    suspend operator fun invoke(category: MovieCategory): Flow<Result<List<Movie>>> {
        return catalogRepository.getMoviesByCategory(category)
    }
}