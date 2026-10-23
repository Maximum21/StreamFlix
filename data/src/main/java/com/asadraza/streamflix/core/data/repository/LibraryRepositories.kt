package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDataErrorType
import com.asadraza.streamflix.core.data.source.CurrentProfileStore
import com.asadraza.streamflix.core.data.source.MyListDataSource
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.data.source.WatchHistoryDataSource
import com.asadraza.streamflix.core.data.util.asResultFlow
import com.asadraza.streamflix.core.data.util.operationFlow
import com.asadraza.streamflix.core.domain.repository.MyListRepository
import com.asadraza.streamflix.core.domain.repository.WatchHistoryRepository
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Observes per-profile data for whichever profile is active, switching when the profile changes. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> CurrentProfileStore.observeFor(whenNone: T, block: (String) -> Flow<T>): Flow<T> =
    currentProfileId.distinctUntilChanged().flatMapLatest { id -> if (id == null) flowOf(whenNone) else block(id) }

private suspend fun CurrentProfileStore.requireProfileId(): String =
    currentProfileId.first() ?: throw UnauthenticatedException()

class MyListRepositoryImpl
@Inject
constructor(
    private val dataSource: MyListDataSource,
    private val currentProfileStore: CurrentProfileStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MyListRepository {
    override fun observeMyList(): Flow<Result<List<MyListItem>>> = currentProfileStore
        .observeFor(emptyList()) { dataSource.observeItems(it) }
        .asResultFlow()
        .flowOn(ioDispatcher)

    override fun observeIsInMyList(movieId: String): Flow<Boolean> = currentProfileStore
        .observeFor(false) { dataSource.observeContains(it, movieId) }
        .flowOn(ioDispatcher)

    override fun add(item: MyListItem): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        dataSource.put(currentProfileStore.requireProfileId(), item)
        Result.Success(Unit)
    }

    override fun remove(movieId: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        dataSource.remove(currentProfileStore.requireProfileId(), movieId)
        Result.Success(Unit)
    }
}

class WatchHistoryRepositoryImpl
@Inject
constructor(
    private val dataSource: WatchHistoryDataSource,
    private val currentProfileStore: CurrentProfileStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {
    override fun observeRecent(): Flow<Result<List<WatchedMovie>>> = currentProfileStore
        .observeFor(emptyList()) { dataSource.observeRecent(it, RECENT_LIMIT) }
        .asResultFlow()
        .flowOn(ioDispatcher)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun record(item: WatchedMovie): Result<Unit> = withContext(ioDispatcher) {
        try {
            dataSource.put(currentProfileStore.requireProfileId(), item)
            Result.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Result.Error(failure, failure.toDataErrorType())
        }
    }

    override fun remove(movieId: String): Flow<Result<Unit>> = operationFlow(ioDispatcher) {
        dataSource.remove(currentProfileStore.requireProfileId(), movieId)
        Result.Success(Unit)
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
