package com.asadraza.streamflix.core.domain.usecase.home

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.model.movie.MovieDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting movie details
 *
 * - only needs detail operations
 * - test: mock only what's needed
 */
class GetMovieDetailsUseCase @Inject constructor(
    private val detailRepository: MovieDetailRepository
) {
    suspend operator fun invoke(movieId: String): Flow<Result<MovieDetail>> {
        return detailRepository.getMovieDetails(movieId)
    }
}