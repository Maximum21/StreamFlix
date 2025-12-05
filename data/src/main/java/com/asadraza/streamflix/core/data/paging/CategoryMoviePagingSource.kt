package com.asadraza.streamflix.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.asadraza.streamflix.core.data.mapper.toDomain
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.network.datasource.RemoteDataSource

class CategoryMoviePagingSource (
    private val remoteDataSource: RemoteDataSource,
    private val category: MovieCategory
) : PagingSource<Int, Movie>() {

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: STARTING_PAGE_INDEX

        return try {
            val response = when (category) {
                MovieCategory.Trending -> remoteDataSource.getTrendingMoviesPaginated(page)
                MovieCategory.Popular -> remoteDataSource.getPopularMoviesPaginated(page)
                MovieCategory.TopRated -> remoteDataSource.getTopRatedMoviesPaginated(page)
                MovieCategory.Upcoming -> remoteDataSource.getUpcomingMoviesPaginated(page)
                MovieCategory.NowPlaying -> remoteDataSource.getNowPlayingMoviesPaginated(page)
                }
            val movies = response.results.toDomain()

            LoadResult.Page(
                data = movies,
                prevKey = if(page == STARTING_PAGE_INDEX) null else page-1,
                nextKey = if(page >= response.totalPages || movies.isEmpty()) null else page+1
            )

        }catch (e: Exception){
            LoadResult.Error(e)
        }
    }

    companion object {
        const val STARTING_PAGE_INDEX = 1
    }

}