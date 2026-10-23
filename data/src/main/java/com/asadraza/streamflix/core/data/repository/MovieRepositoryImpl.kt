package com.asadraza.streamflix.core.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.data.mapper.toDomain
import com.asadraza.streamflix.core.data.mapper.toEntity
import com.asadraza.streamflix.core.data.paging.CategoryMovieRemoteMediator
import com.asadraza.streamflix.core.data.paging.GenreMovieRemoteMediator
import com.asadraza.streamflix.core.data.paging.SearchMovieRemoteMediator
import com.asadraza.streamflix.core.data.util.networkBoundResource
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.domain.repository.MovieDiscoverRepository
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Room is the single source of truth: list screens read from it through a Pager, and the
 * RemoteMediators are the only writers of the list tables. Details use networkBoundResource.
 *
 * Intentionally not @Singleton: it is a stateless forwarder bound to three interfaces. If state
 * is added that is not an injected dependency, scope it in the same change, otherwise each
 * binding would get its own copy.
 */
@OptIn(ExperimentalPagingApi::class)
class MovieRepositoryImpl @Inject constructor(
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) : MovieCatalogRepository,
    MovieDetailRepository,
    MovieSearchRepository,
    MovieDiscoverRepository {
    override fun getMoviesByCategoryPaginated(category: MovieCategory): Flow<PagingData<Movie>> = Pager(
        config = pagingConfig,
        remoteMediator = CategoryMovieRemoteMediator(category, remoteDataSource, localDataSource),
        pagingSourceFactory = { localDataSource.getMoviesByCategoryPagingSource(category.id) }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun searchMoviesPaginated(query: String): Flow<PagingData<Movie>> = Pager(
        config = pagingConfig,
        remoteMediator = SearchMovieRemoteMediator(query, remoteDataSource, localDataSource),
        pagingSourceFactory = { localDataSource.searchMoviesPagingSource(query) }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun discoverMoviesByGenres(genreIds: Set<Int>): Flow<PagingData<Movie>> = Pager(
        config = pagingConfig,
        remoteMediator = GenreMovieRemoteMediator(genreIds, remoteDataSource, localDataSource),
        pagingSourceFactory = {
            localDataSource.getMoviesByCategoryPagingSource(GenreMovieRemoteMediator.listId(genreIds))
        }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun getMovieDetails(movieId: String): Flow<Result<MovieDetail>> {
        // The id also arrives from deep links, so it is not trusted to be a number
        val id = movieId.toIntOrNull() ?: return flowOf(
            Result.Error(IllegalArgumentException("Invalid movie id"), ErrorType.Http(HTTP_NOT_FOUND))
        )
        return detailResource(movieId, id)
    }

    private fun detailResource(movieId: String, id: Int): Flow<Result<MovieDetail>> = networkBoundResource(
        query = {
            localDataSource.getMovieDetailById(movieId).map { entity -> entity?.toDomain() }
        },
        fetch = { remoteDataSource.getMovieDetails(id) },
        saveFetchResult = { dto -> localDataSource.insertMovieDetail(dto.toDomain().toEntity()) },
        shouldFetch = { cachedDetail -> cachedDetail == null }
    )

    internal companion object {
        private const val HTTP_NOT_FOUND = 404

        // Placeholders must stay on with Room's offset-based PagingSource. With them off, Paging's
        // anchor position is an index into the loaded window rather than a database offset, so after
        // a page was saved and the list reloaded, the reload started from an earlier offset and the
        // list jumped back to where it had been scrolled before.
        val pagingConfig = PagingConfig(
            pageSize = Constants.PAGE_SIZE,
            initialLoadSize = Constants.INITIAL_LOAD_SIZE,
            prefetchDistance = Constants.PREFETCH_DISTANCE,
            enablePlaceholders = true
        )
    }
}
