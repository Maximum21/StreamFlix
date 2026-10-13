package com.asadraza.streamflix.core.domain.usecase.home

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for paginated category movie loading
 *
 * Uses Paging 3 for efficient infinite scrolling on Home screen
 *
 * KEY FEATURES:
 * - Category-based loading (Trending, Popular, etc.)
 * - Efficient memory management via Paging 3
 * - Cached in viewModelScope to survive configuration changes
 */
class GetMoviesByCategoryPaginatedUseCase @Inject constructor(
    private val catalogRepository: MovieCatalogRepository
) {
    /**
     * Get paginated movies for a category
     *
     * @param category Movie category (Trending, Popular, etc.)
     * @param scope CoroutineScope for caching (typically viewModelScope)
     * @return Flow of PagingData<Movie>
     */
    operator fun invoke(
        category: MovieCategory,
        scope: CoroutineScope
    ): Flow<PagingData<Movie>> {
        return catalogRepository.getMoviesByCategoryPaginated(category)
            .cachedIn(scope)
    }
}