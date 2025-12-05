package com.asadraza.streamflix.core.domain.usecase.home

import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.model.movie.MovieCategory
import javax.inject.Inject

/**
 * Use case for refreshing movies (pull-to-refresh)
 *
 * Explicit action for forcing network fetch when User pulls to refresh
 * only needs catalog operations
 */
class RefreshMoviesUseCase @Inject constructor(
    private val catalogRepository: MovieCatalogRepository
) {
    suspend operator fun invoke(category: MovieCategory) {
        catalogRepository.refreshMovies(category)
    }
}