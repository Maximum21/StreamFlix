package com.asadraza.streamflix.core.domain.repository

import androidx.paging.PagingData
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import kotlinx.coroutines.flow.Flow

interface MovieCatalogRepository {
    /**
     * Pages of a category, served from the local cache and topped up from the network.
     * The returned flow is cold; collectors should cache it in their own scope.
     */
    fun getMoviesByCategoryPaginated(category: MovieCategory): Flow<PagingData<Movie>>
}
