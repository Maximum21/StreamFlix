package com.asadraza.streamflix.core.domain.usecase.home

import androidx.paging.PagingData
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetMoviesByCategoryPaginatedUseCase
@Inject
constructor(
    private val catalogRepository: MovieCatalogRepository
) {
    operator fun invoke(category: MovieCategory): Flow<PagingData<Movie>> =
        catalogRepository.getMoviesByCategoryPaginated(category)
}
