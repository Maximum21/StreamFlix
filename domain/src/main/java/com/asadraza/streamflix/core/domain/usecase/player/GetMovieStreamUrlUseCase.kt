package com.asadraza.streamflix.core.domain.usecase.player

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving movie streaming URL based on quality.
 *
 * @param movieId Unique identifier of the movie
 * @param quality Quality level (auto, 480p, 720p, 1080p, 2160p)
 * @return Result containing stream URL or error
 */
class GetMovieStreamUrlUseCase @Inject constructor(
    private val movieDetailRepository: MovieDetailRepository
) {
//    suspend operator fun invoke(movieId: String, quality: String): Flow<Result<String>> {
//        return movieDetailRepository.getStreamUrl(movieId, quality)
//    }
}