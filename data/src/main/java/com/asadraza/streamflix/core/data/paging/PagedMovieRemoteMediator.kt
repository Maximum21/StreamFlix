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

    /**
     * Skips the automatic refresh while the stored list is younger than [maxAgeMillis]. Without this,
     * every new Pager (a revisited screen, a changed filter) re-fetched the network and replaced
     * rows that were already on screen. Pull-to-refresh still forces a refresh.
     */
    protected suspend fun initializeWithin(maxAgeMillis: Long): InitializeAction {
        val createdAt = localDataSource.getRemoteKey(remoteKeyId)?.createdAt
        val isFresh = createdAt != null && System.currentTimeMillis() - createdAt < maxAgeMillis
        return if (isFresh) InitializeAction.SKIP_INITIAL_REFRESH else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    protected abstract suspend fun savePage(
        movies: List<MovieEntity>,
        startPosition: Int,
        remoteKey: RemoteKeyEntity,
        clearExisting: Boolean
    )

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MovieEntity>): MediatorResult {
        val storedKey = localDataSource.getRemoteKey(remoteKeyId)
        // An append with nothing stored (the cache was cleared while the list was open) has no next
        // page to continue from, so it starts the list again instead of ending it empty
        val isRefresh = loadType == LoadType.REFRESH || (loadType == LoadType.APPEND && storedKey == null)
        val page = pageToLoad(loadType, isRefresh, storedKey)
            ?: return MediatorResult.Success(endOfPaginationReached = true)

        return try {
            storePage(page, isRefresh, storedKey)
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

    /** The TMDB page to request, or null when there is nothing more to load. */
    private fun pageToLoad(loadType: LoadType, isRefresh: Boolean, storedKey: RemoteKeyEntity?): Int? = when {
        isRefresh -> STARTING_PAGE_INDEX
        loadType == LoadType.PREPEND -> null
        else -> storedKey?.nextKey
    }

    /** Fetches [page] first, then writes it in one transaction, so a failed fetch keeps the cached list. */
    private suspend fun storePage(page: Int, isRefresh: Boolean, storedKey: RemoteKeyEntity?): MediatorResult {
        val response = fetchPage(page)
        val endOfPaginationReached = page >= response.totalPages || response.results.isEmpty()
        val remoteKey = RemoteKeyEntity(
            id = remoteKeyId,
            prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
            nextKey = if (endOfPaginationReached) null else page + 1,
            currentPage = page,
            // Only a refresh renews the freshness clock; appending must not make a stale list look new
            createdAt = if (isRefresh || storedKey == null) System.currentTimeMillis() else storedKey.createdAt
        )
        savePage(
            movies = response.results.map { it.toEntity() },
            startPosition = (page - 1) * Constants.TMDB_PAGE_SIZE,
            remoteKey = remoteKey,
            clearExisting = isRefresh
        )
        return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
    }

    private companion object {
        const val STARTING_PAGE_INDEX = 1
    }
}
