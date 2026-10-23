package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDataErrorType
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.domain.repository.CacheRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CacheRepositoryImpl @Inject constructor(
    private val localDataSource: ILocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CacheRepository {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun clearCache(): Result<Unit> = withContext(ioDispatcher) {
        try {
            localDataSource.clearCache()
            Result.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Result.Error(failure, failure.toDataErrorType())
        }
    }
}
