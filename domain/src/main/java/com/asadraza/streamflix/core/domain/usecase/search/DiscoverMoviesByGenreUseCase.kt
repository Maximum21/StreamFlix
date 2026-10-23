package com.asadraza.streamflix.core.domain.usecase.search

import androidx.paging.PagingData
import com.asadraza.streamflix.core.domain.repository.MovieDiscoverRepository
import com.asadraza.streamflix.core.model.movie.Movie
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Browses by genre. With no genre selected there is nothing to browse, so the result is empty. */
class DiscoverMoviesByGenreUseCase
@Inject
constructor(
    private val discoverRepository: MovieDiscoverRepository
) {
    operator fun invoke(genreIds: Set<Int>): Flow<PagingData<Movie>> =
        if (genreIds.isEmpty()) flowOf(noResults()) else discoverRepository.discoverMoviesByGenres(genreIds)
}
