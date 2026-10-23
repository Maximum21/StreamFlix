package com.asadraza.streamflix.core.data.paging

import android.database.sqlite.SQLiteException
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.data.mapper.toEntity
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.network.model.MovieListResponse
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shared paging logic for list-shaped TMDB endpoints. The network fetch happens before any
 * write, and a page is written in one transaction, so a failed refresh keeps the cached list.
 *
 * One remote key per list (not per movie) tracks the next page.
 */
@OptIn(ExperimentalPagingApi::class)
abstract class PagedMovieRemoteMediator(
    private val remoteKeyId: String,
    private val localDataSource: ILocalDataSource
) : RemoteMediator<Int, MovieEntity>() {
    protected abstract suspend fun fetchPage(page: Int): MovieListResponse

    protected abstract suspend fun savePage(
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    )

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MovieEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> STARTING_PAGE_INDEX
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND ->
                localDataSource.getRemoteKey(remoteKeyId)?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val response = fetchPage(page)
            val endOfPaginationReached = page >= response.totalPages || response.results.isEmpty()
            val remoteKey = RemoteKeyEntity(
                id = remoteKeyId,
                prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (endOfPaginationReached) null else page + 1,
                currentPage = page
            )
            savePage(
                movies = response.results.map { it.toEntity() },
                startPosition = (page - 1) * Constants.TMDB_PAGE_SIZE,
                remoteKey = remoteKey,
                clearExisting = loadType == LoadType.REFRESH
            )
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NetworkError) {
            MediatorResult.Error(e)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: SQLiteException) {
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val STARTING_PAGE_INDEX = 1
    }
}
