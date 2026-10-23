package com.asadraza.streamflix.core.domain.repository

import androidx.paging.PagingData
import com.asadraza.streamflix.core.model.movie.Movie
import kotlinx.coroutines.flow.Flow

interface MovieDiscoverRepository {
    /** Pages of movies in any of [genreIds]. The returned flow is cold; collectors should cache it. */
    fun discoverMoviesByGenres(genreIds: Set<Int>): Flow<PagingData<Movie>>
}
