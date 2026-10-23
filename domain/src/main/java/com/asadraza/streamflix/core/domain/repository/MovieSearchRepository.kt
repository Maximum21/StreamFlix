package com.asadraza.streamflix.core.domain.repository

import androidx.paging.PagingData
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.Flow

interface MovieSearchRepository {
    /** Pages of results for [query]. The returned flow is cold; collectors should cache it. */
    fun searchMoviesPaginated(query: String): Flow<PagingData<Movie>>
}
